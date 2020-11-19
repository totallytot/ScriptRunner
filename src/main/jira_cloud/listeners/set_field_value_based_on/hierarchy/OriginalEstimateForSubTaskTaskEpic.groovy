package jira_cloud.listeners.set_field_value_based_on.hierarchy

import kong.unirest.Unirest

/**
 * ### Hierarchy ###
 * Sub-task -> Issue in Epic -> Epic
 * For Epic:
 * - 'Original Estimate' = Sum 'Original Estimate' of all Issues in Epic;
 * ### If issue in Epic has subtasks, then 'Original Estimates' and statuses of subtasks should be considered. Parent task should not be counted. Use hours ###:
 * - 'Estimate To Do' = Sum 'Original Estimate' of all issues in Epic in 'To Do' status;
 * - 'Estimate In Progress' = Sum 'Original Estimate' of all issues in Epic in 'In Progress' status;
 * - 'Estimate Issues' = Sum 'Original Estimate' of all issues in Epic in 'Issues' status;
 * - 'Estimate In Testing' = Sum 'Original Estimate' of all issues in Epic in 'In Testing' status;
 * - 'Estimate Future Tasks' = Sum 'Original Estimate' of all issues in Epic in 'Future Tasks' status;
 * - 'Estimate Completed' = Sum 'Original Estimate' of all issues in Epic in 'Completed' status.
 * For issue in Epic:
 * - with sub-tasks: 'Original Estimate' = Sum 'Original Estimate' of all subtasks in this issue;
 * - w/t sub-tasks: 'Original Estimate' is set manually.
 * ### Triggers ###
 * - Original Estimation fields update;
 * - any issue is added/removed/reassigned to/from/between Epic/s;
 * - issue created with 'Original Estimate';
 * - status change.
 */

final String EPIC_LINK = "Epic Link"
final String ORIGINAL_ESTIMATE = "timeoriginalestimate"

final String ESTIMATE_TO_DO = "Estimate To Do"
final String ESTIMATE_IN_PROGRESS = "Estimate In Progress"
final String ESTIMATE_ISSUES = "Estimate Issues"
final String ESTIMATE_IN_TESTING = "Estimate In Testing"
final String ESTIMATE_FUTURE_TASKS = "Estimate Future Tasks"
final String ESTIMATE_COMPLETED = "Estimate Completed"

def issueType = issue.fields.issuetype.name as String
if (issueType == "Epic") return

def customFields = customFields
def epicLinkId = customFields.find { it.name == EPIC_LINK }?.id
def estimateToDoId = customFields.find { it.name == ESTIMATE_TO_DO }?.id
def estimateInProgressId = customFields.find { it.name == ESTIMATE_IN_PROGRESS }?.id
def estimateIssuesId = customFields.find { it.name == ESTIMATE_ISSUES }?.id
def estimateInTestingId = customFields.find { it.name == ESTIMATE_IN_TESTING }?.id
def estimateFutureTasksId = customFields.find { it.name == ESTIMATE_FUTURE_TASKS }?.id
def estimateCompletedId = customFields.find { it.name == ESTIMATE_COMPLETED }?.id

def isSubTask = issue.fields.issuetype.subtask as Boolean
def originalEstimateChange = changelog?.items?.find { (it as Map).field == ORIGINAL_ESTIMATE } as Map
def epicLinkChange = changelog?.items?.find { (it as Map).field == EPIC_LINK } as Map
def statusChange = changelog?.items?.find { (it as Map).field == "status" } as Map
def wasCreatedWithOriginalEstimate = issue_event_type_name == "issue_created" && issue.fields[ORIGINAL_ESTIMATE] > 0

def epicOriginalEstimateCalculation = { String epicKey ->
    def jqlIssuesInEpic = """ "${EPIC_LINK}" = ${epicKey} """
    def issuesInEpic = executeSearch(jqlIssuesInEpic, 0, 500) as List

    def epicOriginalEstimateVal = issuesInEpic?.findResults { it.fields[ORIGINAL_ESTIMATE] }?.sum()
    if (epicOriginalEstimateVal) epicOriginalEstimateVal /= 60
    setOriginalEstimate(epicKey, epicOriginalEstimateVal)

    def issuesForEstimateCalculation = new ArrayList<Map>()
    issuesInEpic.each { Map issueInEpic ->
        def subTaskKeys = issueInEpic.fields.subtasks*.key
        if (subTaskKeys.empty) issuesForEstimateCalculation << issueInEpic
        else subTaskKeys.each { issuesForEstimateCalculation << getIssue(it) }
    }
    def fieldsValsMapping = [:]

    def estimateToDoVal = issuesForEstimateCalculation.findResults {
        def status = it.fields.status.name as String
        status == "To Do" ? it.fields[ORIGINAL_ESTIMATE] : null
    }?.sum()
    if (estimateToDoVal) estimateToDoVal /= 3600
    fieldsValsMapping.put(estimateToDoId, estimateToDoVal)

    def estimateInProgressVal = issuesForEstimateCalculation.findResults {
        def status = it.fields.status.name as String
        status == "In Progress" ? it.fields[ORIGINAL_ESTIMATE] : null
    }?.sum()
    if (estimateInProgressVal) estimateInProgressVal /= 3600
    fieldsValsMapping.put(estimateInProgressId, estimateInProgressVal)

    def estimateIssuesVal = issuesForEstimateCalculation.findResults {
        def status = it.fields.status.name as String
        status == "Issues" ? it.fields[ORIGINAL_ESTIMATE] : null
    }?.sum()
    if (estimateIssuesVal) estimateIssuesVal /= 3600
    fieldsValsMapping.put(estimateIssuesId, estimateIssuesVal)

    def estimateInTestingVal = issuesForEstimateCalculation.findResults {
        def status = it.fields.status.name as String
        status == "In Testing" ? it.fields[ORIGINAL_ESTIMATE] : null
    }?.sum()
    if (estimateInTestingVal) estimateInTestingVal /= 3600
    fieldsValsMapping.put(estimateInTestingId, estimateInTestingVal)

    def estimateFutureTasksVal = issuesForEstimateCalculation.findResults {
        def status = it.fields.status.name as String
        status == "Future Tasks" ? it.fields[ORIGINAL_ESTIMATE] : null
    }?.sum()
    if (estimateFutureTasksVal) estimateFutureTasksVal /= 3600
    fieldsValsMapping.put(estimateFutureTasksId, estimateFutureTasksVal)

    def estimateCompletedVal = issuesForEstimateCalculation.findResults {
        def status = it.fields.status.name as String
        status == "Completed" ? it.fields[ORIGINAL_ESTIMATE] : null
    }?.sum()
    if (estimateCompletedVal) estimateCompletedVal /= 3600
    fieldsValsMapping.put(estimateCompletedId, estimateCompletedVal)

    setFields(epicKey, fieldsValsMapping)
}

def parentOriginalEstimateCalculation = { Map parentIssue ->
    def subTasksKeys = parentIssue.fields.subtasks*.key
    def parentOriginalEstimateVal = subTasksKeys.findResults { getIssue(it).fields[ORIGINAL_ESTIMATE] }.sum()
    if (parentOriginalEstimateVal) parentOriginalEstimateVal /= 60
    setOriginalEstimate(parentIssue.key, parentOriginalEstimateVal)
}

def findAffectedEpics = { Map regularIssue ->
    def epicKeys = []
    if (epicLinkChange) {
        epicKeys << epicLinkChange.fromString
        epicKeys << epicLinkChange.toString
    } else epicKeys << regularIssue.fields[epicLinkId]
    epicKeys.removeAll { it == null }
    return epicKeys
}

if (isSubTask) {
    def shouldRun = originalEstimateChange || statusChange
    logger.info "${issue.key} shouldRun: ${shouldRun}"
    if (!shouldRun) return
    def parentIssueKey = issue.fields.parent.key
    def parentIssue = getIssue(parentIssueKey)
    parentOriginalEstimateCalculation(parentIssue)
    def epicIssueKey = parentIssue.fields[epicLinkId]
    if (epicIssueKey) epicOriginalEstimateCalculation(epicIssueKey)
} else {
    def shouldRun = originalEstimateChange || epicLinkChange || wasCreatedWithOriginalEstimate || statusChange
    logger.info "${issue.key} shouldRun: ${shouldRun}"
    if (!shouldRun) return
    def epics = findAffectedEpics(issue)
    epics.each { epicOriginalEstimateCalculation(it) }
}

static List<Map> getCustomFields() {
    Unirest.get("/rest/api/2/field")
            .header('Content-Type', 'application/json')
            .asObject(List)
            .body
            .findAll { (it as Map).custom } as List<Map>
}

static Map getIssue(String issueKey) {
    Unirest.get("/rest/api/2/issue/${issueKey}")
            .header('Content-Type', 'application/json')
            .asObject(Map).body
}

static setOriginalEstimate(issueKey, minutes) {
    Unirest.put("/rest/api/2/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: [timetracking: [originalEstimate: "${minutes}m"]]])
            .asString()
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