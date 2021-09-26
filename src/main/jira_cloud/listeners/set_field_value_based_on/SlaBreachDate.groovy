package jira_cloud.listeners.set_field_value_based_on

import kong.unirest.Unirest

enum SlaFieldMapping {
    SLA_00("CR 3d", "customfield_11676", 10004), // CR - CR_target_3
    SLA_01("CR 7d", "customfield_11677", 10004), // // CR - CR_target_7
    SLA_02("LAW 7d", "customfield_11672", 10000), // Law - law_target_7
    SLA_03("LAW 15d", "customfield_11673", 10000), // Law - law_target_15
    SLA_04("LAW 25d", "customfield_11674", 10000), // Law - law_target_25
    SLA_05("Law AR 7d", "customfield_11687", 10002), // Law AR - law_AR_target_7
    SLA_06("Law AR 15d", "customfield_11688", 10002) // // Law AR - law_AR_target_15

    final String slaName
    final String fieldId
    final int issueTypeId

    SlaFieldMapping(String slaName, String fieldId, int issueTypeId) {
        this.slaName = slaName
        this.fieldId = fieldId
        this.issueTypeId = issueTypeId
    }

    static String getFieldIdBySlaName(String slaName) {
        this.find { it.slaName == slaName }?.fieldId
    }

    static boolean isAllowedIssueType(int issueTypeId) {
        this.any { it.issueTypeId == issueTypeId }
    }
}

logger.info "SLA BREACH DATE LISTENER START"

logger.info "Working with ${issue.key}"
def isAllowedIssueType = SlaFieldMapping.isAllowedIssueType(issue.fields.issuetype.id as int)
if (!isAllowedIssueType) {
    logger.info "Condition not passed - listener stopped"
    return
}
logger.info "Condition passed"

def collectActualSlaData = {
    def slaDataResult = getSlaData(issue.key as String)
    if (slaDataResult.status != 200) {
        logger.error "Error during getting SLA breach time - listener stopped: ${slaDataResult}"
        return
    }
    logger.info "slaDataResult: ${slaDataResult.body.values}"
    return slaDataResult.body.values.findAll {
        it.name.toString() in SlaFieldMapping.values().collect { it.slaName }
    }?.findAll { it.ongoingCycle?.breachTime }
}

// do several attempts as SLA has delays in setting breach time
int slaRequestCount = 1
def actualSLAs = []
while(actualSLAs.empty) {
    logger.info "Request SLA attempt: ${slaRequestCount}"
    actualSLAs = collectActualSlaData()
    logger.info "SLA exists: ${!actualSLAs.empty}"
    slaRequestCount++
    if (slaRequestCount > 4) {
        logger.info "Sla request count breached"
        break
    }
    sleep(3000)
}

if (actualSLAs.empty) {
    logger.info "No actual SLAs in mapping - listener stopped"
    return
}

def fieldValuesMapping = actualSLAs.collectEntries {
    def slaBreachTime = it.ongoingCycle.breachTime.jira
    def field = SlaFieldMapping.getFieldIdBySlaName(it.name.toString())
    if (field && slaBreachTime) return [field, slaBreachTime]
}

if (fieldValuesMapping.keySet().empty) {
    logger.error "Error during values generation from SLA breach time - listener stopped"
    return
}
logger.info "Field values based on SLA breach time: ${fieldValuesMapping.toString()}"

def result = setFields(issue.key as String, fieldValuesMapping)
if (result.status != 204) logger.error "Error during fields update: ${result.status} ${result.body}"

logger.info "SLA BREACH DATE LISTENER END"

static def getSlaData(String issueKey) {
    Unirest.get("/rest/servicedeskapi/request/${issueKey}/sla")
            .header("Content-Type", "application/json")
            .asObject(Map)
}

static def setFields(String issueKey, Map fieldValueMapping) {
    Unirest.put("/rest/api/3/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: fieldValueMapping]).asString()
}