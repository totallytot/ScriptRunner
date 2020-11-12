package jira_cloud.listeners.set_field_value_based_on.hierarchy

import kong.unirest.Unirest

final String E_H_N_L = "ESTIMATION: HOURS NIKLAUS LÜTHY"
final String B_C_N_L = "BUDGETED: COST NIKLAUS LÜTHY"
final String E_H_S_D = "ESTIMATION: HOURS SMARCOM DEVELOPMENT"
final String B_S_D = "BUDGETED: SMARCOM DEVELOPMENT"
final String E_H_S_P_S = "ESTIMATION: HOURS SMARCOM PROJECT SUPPORT"
final String B_S_P_S = "BUDGETED: SMARCOM PROJECT SUPPORT"
final String EPIC_LINK = "Epic Link"

def customFields = Unirest.get("/rest/api/2/field").asObject(List).body.findAll { (it as Map).custom } as List<Map>
def ehnlId = customFields.find { it.name == E_H_N_L }?.id
def bcnlId = customFields.find { it.name == B_C_N_L }?.id
def ehsdId = customFields.find { it.name == E_H_S_D }?.id
def bsdId = customFields.find { it.name == B_S_D }?.id
def ehspsId = customFields.find { it.name == E_H_S_P_S }?.id
def bspsId = customFields.find { it.name == B_S_P_S }?.id
def epicLinkId = customFields.find { it.name == EPIC_LINK }?.id

def changedFields = null
def isIssueCreatedEvent = false
switch (issue_event_type_name) {
    case "issue_updated":
        changedFields = changelog.items.field as List<String>
        break
    case "issue_created":
        isIssueCreatedEvent = true
        break
}

def isSubTask = issue.fields.issuetype.subtask as Boolean
def issueType = issue.fields.issuetype.name as String

if (issueType == "Epic") return
def epicLinkChange = changelog?.items?.find { (it as Map).field == EPIC_LINK } as Map

def epicKeys = []
epicKeys << issue.fields[epicLinkId]
if (epicLinkChange) {
    epicKeys << epicLinkChange.fromString
    epicKeys << epicLinkChange.toString
}
epicKeys.removeAll { it == null }
def parentIssueKey = null
def parentIssue = null
def subTasksKeys = null
if (isSubTask) {
    parentIssueKey = issue.fields.parent.key
    parentIssue = getIssue(parentIssueKey)
    subTasksKeys = parentIssue.fields.subtasks*.key
}

if (changedFields?.any { it in [E_H_N_L, EPIC_LINK] } || isIssueCreatedEvent) {
    // Task and Sub-Task calculation: BUDGETED: COST NIKLAUS LÜTHY = ESTIMATION: HOURS NIKLAUS LÜTHY * 105
    def enhlVal = issue.fields[ehnlId]
    def bcnlVal = null
    if (enhlVal || enhlVal == 0) bcnlVal = enhlVal * 105
    logger.info "${issueType} ${issue.key} bcnlVal: ${bcnlVal}"
    setField(issue.key, bcnlId, bcnlVal)
    if (isSubTask) {
        // Parent calculation: BUDGETED: COST NIKLAUS LÜTHY = SUM of subtasks BUDGETED: NIKLAUS LÜTHY
        def bcnlValParent = subTasksKeys.findResults { getIssue(it).fields[bcnlId] }.sum()
        logger.info "Parrent Issue: ${parentIssueKey} bcnlValParent: ${bcnlValParent}"
        // Parent calculation: ESTIMATION: HOURS  NIKLAUS LÜTHY = SUM of subtasks ESTIMATION: HOURS NIKLAUS LÜTHY
        def enhlValParent = subTasksKeys.findResults { getIssue(it).fields[ehnlId] }.sum()
        logger.info "Parrent Issue: ${parentIssueKey} enhlValParent: ${enhlValParent}"
        def parentFieldsVals = [:]
        parentFieldsVals.put(bcnlId, bcnlValParent)
        parentFieldsVals.put(ehnlId, enhlValParent)
        setFields(parentIssueKey, parentFieldsVals)
        epicKeys << parentIssue.fields[epicLinkId]
    }
    epicKeys.each { epicKey ->
        // Epic calculation: BUDGETED: COST NIKLAUS LÜTHY = SUM of issues in epic BUDGETED: NIKLAUS LÜTHY
        def jqlIssuesInEpic = """ "${EPIC_LINK}" = ${epicKey} """
        def issuesInEpic = executeSearch(jqlIssuesInEpic, 0, 500) as List
        def bcnlValEpic = issuesInEpic.findResults { it.fields[bcnlId] }.sum()
        logger.info "Epic ${epicKey} bcnlValEpic: ${bcnlValEpic}"
        // Epic calculation: ESTIMATION: HOURS NIKLAUS LÜTHY = SUM of issues in epic ESTIMATION: HOURS NIKLAUS LÜTHY
        def enhlValEpic = issuesInEpic.findResults { it.fields[ehnlId] }.sum()
        logger.info "Epic ${epicKey} enhlValEpic: ${enhlValEpic}"
        def epicFieldsVals = [:]
        epicFieldsVals.put(bcnlId, bcnlValEpic)
        epicFieldsVals.put(ehnlId, enhlValEpic)
        setFields(epicKey, epicFieldsVals)
    }
}

if (changedFields?.any { it in [E_H_S_D, EPIC_LINK] } || isIssueCreatedEvent) {
    // Task and Sub-Task calculation: BUDGETED: SMARCOM DEVELOPMENT = ESTIMATION: HOURS SMARCOM DEVELOPMENT * 60
    def ehsdVal = issue.fields[ehsdId]
    def bsdVal = null
    if (ehsdVal || ehsdVal == 0) bsdVal = ehsdVal * 60
    logger.info "${issueType} ${issue.key} bsdVal: ${bsdVal}"
    setField(issue.key, bsdId, bsdVal)
    if (isSubTask) {
        // Parent calculation: BUDGETED: SMARCOM DEVELOPMENT = SUM of subtasks BUDGETED: SMARCOM DEVELOPMENT
        def bsdValParent = subTasksKeys.findResults { getIssue(it).fields[bsdId] }.sum()
        logger.info "Parrent Issue: ${parentIssueKey} bsdValParent: ${bsdValParent}"
        // Parent calculation: ESTIMATION: HOURS SMARCOM DEVELOPMENT = SUM of subtasks ESTIMATION: HOURS SMARCOM DEVELOPMENT
        def ehsdValParent = subTasksKeys.findResults { getIssue(it).fields[ehsdId] }.sum()
        logger.info "Parrent Issue: ${parentIssueKey} ehsdValParent: ${ehsdValParent}"
        def parentFieldsVals = [:]
        parentFieldsVals.put(bsdId, bsdValParent)
        parentFieldsVals.put(ehsdId, ehsdValParent)
        setFields(parentIssueKey, parentFieldsVals)
        epicKeys << parentIssue.fields[epicLinkId]
    }
    epicKeys.each { epicKey ->
        // Epic calculation: BUDGETED: SMARCOM DEVELOPMENT = SUM of issues in epic BUDGETED: SMARCOM DEVELOPMENT
        def jqlIssuesInEpic = """ "${EPIC_LINK}" = ${epicKey} """
        def issuesInEpic = executeSearch(jqlIssuesInEpic, 0, 500) as List
        def bsdValEpic = issuesInEpic.findResults { it.fields[bsdId] }.sum()
        logger.info "Epic ${epicKey} bcmlValEpic: ${bsdValEpic}"
        // Epic calculation: ESTIMATION: HOURS SMARCOM DEVELOPMENT = SUM of issues in epic ESTIMATION: HOURS SMARCOM DEVELOPMENT
        def ehsdValEpic = issuesInEpic.findResults { it.fields[ehsdId] }.sum()
        logger.info "Epic ${epicKey} enhlValEpic: ${ehsdValEpic}"
        def epicFieldsVals = [:]
        epicFieldsVals.put(bsdId, bsdValEpic)
        epicFieldsVals.put(ehsdId, ehsdValEpic)
        setFields(epicKey, epicFieldsVals)
    }
}

if (changedFields?.any { it in [E_H_S_P_S, EPIC_LINK] } || isIssueCreatedEvent) {
    // Task and Sub-Task calculation: BUDGETED: SMARCOM PROJECT SUPPORT = ESTIMATION: HOURS SMARCOM PROJECT SUPPORT * 60
    def ehspsVal = issue.fields[ehspsId]
    def bspsVal = null
    if (ehspsVal || ehspsVal == 0) bspsVal = ehspsVal * 60
    logger.info "${issueType} ${issue.key} bspsVal: ${bspsVal}"
    setField(issue.key, bspsId, bspsVal)
    if (isSubTask) {
        // Parent calculation: BUDGETED: SMARCOM PROJECT SUPPORT = SUM of subtasks BUDGETED: SMARCOM PROJECT SUPPORT
        def bspsValParent = subTasksKeys.findResults { getIssue(it).fields[bspsId] }.sum()
        logger.info "Parrent Issue: ${parentIssueKey} bspsValParent: ${bspsValParent}"
        // Parent calculation: ESTIMATION: HOURS SMARCOM PROJECT SUPPORT = SUM of subtasks ESTIMATION: HOURS SMARCOM PROJECT SUPPORT
        def ehspsValParent = subTasksKeys.findResults { getIssue(it).fields[ehspsId] }.sum()
        logger.info "Parrent Issue: ${parentIssueKey} ehspsValParent: ${ehspsValParent}"
        def parentFieldsVals = [:]
        parentFieldsVals.put(bspsId, bspsValParent)
        parentFieldsVals.put(ehspsId, ehspsValParent)
        setFields(parentIssueKey, parentFieldsVals)
        epicKeys << parentIssue.fields[epicLinkId]
    }
    epicKeys.each { epicKey ->
        // Epic calculation: BUDGETED: SMARCOM PROJECT SUPPORT = SUM of issues in epic BUDGETED: SMARCOM PROJECT SUPPORT
        def jqlIssuesInEpic = """ "${EPIC_LINK}" = ${epicKey} """
        def issuesInEpic = executeSearch(jqlIssuesInEpic, 0, 500) as List
        def bspsValEpic = issuesInEpic.findResults { it.fields[bspsId] }.sum()
        logger.info "Epic ${epicKey} bcmlValEpic: ${bspsValEpic}"
        // Epic calculation: ESTIMATION: HOURS SMARCOM PROJECT SUPPORTT = SUM of issues in epic ESTIMATION: HOURS SMARCOM PROJECT SUPPORT
        def ehspsValEpic = issuesInEpic.findResults { it.fields[ehspsId] }.sum()
        logger.info "Epic ${epicKey} enhlValEpic: ${ehspsValEpic}"
        def epicFieldsVals = [:]
        epicFieldsVals.put(bspsId, bspsValEpic)
        epicFieldsVals.put(ehspsId, ehspsValEpic)
        setFields(epicKey, epicFieldsVals)
    }
}

static Map getIssue(String issueKey) {
    Unirest.get("/rest/api/2/issue/${issueKey}").asObject(Map).body
}

static int setField(String issueKey, String customfield_id, value) {
    def result = Unirest.put("/rest/api/2/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: [(customfield_id): value]]).asString()
    return result.status
}

static int setFields(String issueKey, Map fieldsAndVals) {
    def result = Unirest.put("/rest/api/2/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: fieldsAndVals]).asString()
    return result.status
}

static List executeSearch(String jqlQuery, int startAt, int maxResults) {
    def searchRequest = Unirest.get("/rest/api/2/search")
            .queryString("jql", jqlQuery)
            .queryString("startAt", startAt)
            .queryString("maxResults", maxResults)
            .asObject(Map)
    searchRequest.status == 200 ? searchRequest.body.issues as List : null
}