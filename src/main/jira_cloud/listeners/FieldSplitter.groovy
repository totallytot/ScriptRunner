package jira_cloud.listeners

import kong.unirest.Unirest

/**
 * A Jira issue is created automatically by a webhook from freshservice, and the input information from freshservice
 * is dumped into the field "Freshservice input". The script that reads that field and spreads the information into
 * other fields in Jira based on mapping. Some transmits to text fields but others need to "select fields". Also, if a
 * field goes into a "select field" and the option does not exist already, then the option is added to the select list.
 */

final String FS_INPUT = "Freshservice Input"
final String FS_TICKET = "Freshservice Ticket"
final String DESCRIPTION = "description"
final String FS_TICKET_REPORTER = "Freshservice ticket reporter"
final String FS_PRIORITY = "Freshservice priority"
final String SYSTEM = "System"
final String FS_CLINICAL_SAFETY = "Freshservice clinical safety"
final String COVID_TECH_ISSUE = "Covid19 Tech issue"
final String PRODUCTS_FUNCTIONALITY = "Products/Functionality"
final String FS_SOURCE = "Freshservice source"
final String REGION = "Region"
final String COUNTRIES = "Country(s)"

def customFields = customFields
def fsInputId = customFields.find { it.name == FS_INPUT }?.id
def fsTicketId = customFields.find { it.name == FS_TICKET }?.id
def fsTicketReporterId = customFields.find { it.name == FS_TICKET_REPORTER }?.id
def fsPriorityId = customFields.find { it.name == FS_PRIORITY }?.id
def systemId = customFields.find { it.name == SYSTEM }?.id
def fsClinicalSafetyId = customFields.find { it.name == FS_CLINICAL_SAFETY }?.id
def covidTechIssueId = customFields.find { it.name == COVID_TECH_ISSUE }?.id
def productsFunctionalityId = customFields.find { it.name == PRODUCTS_FUNCTIONALITY }?.id
def fsSourceId = customFields.find { it.name == FS_SOURCE }?.id
def regionId = customFields.find { it.name == REGION }?.id
def countriesId = customFields.find { it.name == COUNTRIES }?.id

//condition
def fsInputVal = issue.fields[fsInputId] as String
if (!fsInputVal) return

def editMetaData = getEditMetaData(issue.key)

def isAllowedVal = { String customfieldId, String value ->
    def allowedVals = editMetaData.fields[customfieldId].allowedValues.value as List
    return value in allowedVals
}

def fsInputList = fsInputVal.split("\r\n")*.trim()
logger.info "fsInputList: ${fsInputList}"
def fieldsValsMapping = [:]

//text fields
fieldsValsMapping.put(fsTicketId, fsInputList[0].find("(?<=Ticket URL:).*\$").trim())
fieldsValsMapping.put(DESCRIPTION, fsInputList[1].find("(?<=Description:).*\$").trim())
fieldsValsMapping.put(fsTicketReporterId, fsInputList[2].find("(?<=Requester:).*\$").trim())
fieldsValsMapping.put(fsPriorityId, fsInputList[3].find("(?<=Priority:).*\$").trim())
fieldsValsMapping.put(systemId, fsInputList[4].find("(?<=What’s broken:).*\$").trim())

//cascading select list
def parentVal = fsInputList[5].find("(?<=Which part:).*\$").trim()
def childVal = fsInputList[6].find("(?<=Which Functionality\\?:).*\$").trim()
fieldsValsMapping.put(productsFunctionalityId, ["value": parentVal, "child": ["value": childVal]])

//single select lists
fieldsValsMapping.put(fsClinicalSafetyId, ["value": fsInputList[7].find("(?<=Clinical Safety:).*\$").trim()])
fieldsValsMapping.put(covidTechIssueId, ["value": fsInputList[13].find("(?<=Covid19 Tech Issue:).*\$").trim()])

//multi select lists
def regionVal = fsInputList[8].find("(?<=Region impacted:).*\$").split(",").findResults { ["value": it.trim()] }
fieldsValsMapping.put(regionId, regionVal)
def fsSourceVal = fsInputList[12].find("(?<=Source:).*\$").split(",").findResults { ["value": it.trim()] }
fieldsValsMapping.put(fsSourceId, fsSourceVal)

//multi select list with adding non-existing options
def countriesVal = []
countriesVal << fsInputList[9].find("(?<=Country impacted:).*\$").trim()
def selectedCountries = fsInputList[11].find("(?<=Selected countries:).*\$")
if (selectedCountries) {
    def countries = selectedCountries.split(",")*.trim()
    countriesVal.addAll(countries)
}
def notAllowedVals = countriesVal.findResults { isAllowedVal(countriesId, it) ? null : it.toString().trim() }
if (!notAllowedVals.empty) addOptions(countriesId, notAllowedVals)
countriesVal = countriesVal.findResults { ["value": it.toString().trim()] }
fieldsValsMapping.put(countriesId, countriesVal)

setFields(issue.key, fieldsValsMapping)

static Map getEditMetaData(String issueKey) {
    Unirest.get("/rest/api/2/issue/${issueKey}/editmeta")
            .header('Content-Type', 'application/json')
            .asObject(Map).body
}

static List<Map> getCustomFields() {
    Unirest.get("/rest/api/2/field")
            .header('Content-Type', 'application/json')
            .asObject(List)
            .body
            .findAll { (it as Map).custom } as List<Map>
}

static int setFields(String issueKey, Map fieldsAndVals) {
    def result = Unirest.put("/rest/api/2/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: fieldsAndVals]).asString()
    return result.status
}

static int addOptions(String customFieldId, List<String> options) {
    customFieldId = customFieldId.replace("customfield_", "")
    options = options.findResults { ["value": it.toString().trim()] }
    def result = Unirest.post("/rest/api/3/customField/${customFieldId}/option")
            .header("Content-Type", "application/json")
            .body(["options": options]).asString()
    return result.status
}