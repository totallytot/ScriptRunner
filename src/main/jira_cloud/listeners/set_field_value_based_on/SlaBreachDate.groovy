package jira_cloud.listeners.set_field_value_based_on

import kong.unirest.Unirest

import java.time.DayOfWeek
import java.time.LocalDate

enum SlaFieldMapping {
    SLA_3("CR 3d", 3, "customfield_11676", "CR_target_3"),
    SLA_7("CR 7d", 7, "customfield_11677", "CR_target_7"),

    final String slaName
    final int days
    final String fieldId
    final String fieldName // just for info

    SlaFieldMapping(String slaName, int days, String fieldId, String fieldName) {
        this.slaName = slaName
        this.fieldId = fieldId
        this.fieldName = fieldName
        this.days = days
    }

    static String getFieldIdBySlaName(String slaName) {
        this.find { it.slaName == slaName }?.fieldId
    }

    static int getDaysByFieldId(String fieldId) {
        this.find { it.fieldId == fieldId }?.days
    }
}

final String LAW_DATE_FIELD_ID = "customfield_11645" // law_date
final String LAW_DUE_DATE_FIELD_ID = "customfield_11646" // law_due_date
final String TRIGGER_ISSUE_TYPE_ID = "10004" // CR

logger.info "SLA BREACH DATE LISTENER START"

logger.info "Working with ${issue.key}"
def isAllowedIssueType = issue.fields.issuetype.id == TRIGGER_ISSUE_TYPE_ID
def isLawDateChange = changelog.items.any { Map changeItem ->
    (changeItem.fieldId as String) in [LAW_DATE_FIELD_ID, LAW_DUE_DATE_FIELD_ID]
}
if (!isAllowedIssueType && !isLawDateChange) {
    logger.info "Condition not passed - listener stopped"
    return
}
logger.info "Condition passed"
def breachTimeValues = generateFieldValuesBasedOnSlaBreachTime(issue.key)
if (!breachTimeValues) {
    logger.error "Error during values generation from SLA breach time - listener stopped"
    return
}
logger.info "Field values based on SLA breach time: ${breachTimeValues.toString()}"
def isNotFull = breachTimeValues.values().any { it == null }
logger.info "Fully Generated: ${!isNotFull}"

def lawDate = issue.fields[LAW_DATE_FIELD_ID]
def lawDueDate = issue.fields[LAW_DUE_DATE_FIELD_ID]

def selectedDate = null
if (!lawDueDate && lawDate && isNotFull) {
    logger.info "Generating values based on law_date"
    selectedDate = LocalDate.parse(lawDate as String)
} else if (lawDueDate && isNotFull) {
    logger.info "Generating values based on law_due_date"
    selectedDate = LocalDate.parse(lawDueDate as String)
}
logger.info "selectedDate: ${selectedDate}"

def fieldValuesMapping = [:]
if (selectedDate) fieldValuesMapping = breachTimeValues.collectEntries { customfieldId, value ->
        if (!value) {
            def daysToAdd = SlaFieldMapping.getDaysByFieldId(customfieldId as String)
            logger.info "daysToAdd: ${daysToAdd}"
            value = addDaysSkippingWeekends(selectedDate, daysToAdd)
            logger.info "Generated: ${customfieldId}: ${value}"
        }
        [customfieldId, value.toString()]
    }
else fieldValuesMapping = breachTimeValues
logger.info "fieldValuesMapping ${fieldValuesMapping}"

def result = setFields(issue.key as String, fieldValuesMapping)
if (result.status != 204) logger.error "Error during fields update: ${result.status} ${result.body}"
logger.info "SLA BREACH DATE LISTENER END"

static Map generateFieldValuesBasedOnSlaBreachTime(String issueKey) {
    def result = Unirest.get("/rest/servicedeskapi/request/${issueKey}/sla")
            .header("Content-Type", "application/json")
            .asObject(Map)
    if (result.status == 200) {
        def actualSLAs = result.body.values.findAll { it.name in SlaFieldMapping.values().collect { it.slaName } }
        actualSLAs.collectEntries {
            [SlaFieldMapping.getFieldIdBySlaName(it.name as String), it.ongoingCycle?.breachTime?.jira]
        }
    } else null
}

static LocalDate addDaysSkippingWeekends(LocalDate date, int days) {
    LocalDate result = date
    int addedDays = 0
    while (addedDays < days) {
        result = result.plusDays(1)
        // Israel
        if (!(result.dayOfWeek in [DayOfWeek.FRIDAY, DayOfWeek.SATURDAY])) ++addedDays
    }
    result
}

static def setFields(String issueKey, Map fieldValueMapping) {
    Unirest.put("/rest/api/3/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: fieldValueMapping]).asString()
}