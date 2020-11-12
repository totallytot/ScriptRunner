package jira_cloud.listeners.set_field_value_based_on.hierarchy

import kong.unirest.Unirest

/**
 * ### Hierarchy ###
 * Sub-task -> Issue in Epic -> Epic
 * For Epic:
 * - 'Original Estimate' = Sum 'Original Estimate' of all Issues in Epic.
 * For issue in Epic:
 * - with sub-tasks: 'Original Estimate' = Sum 'Original Estimate' of all subtasks in this issue;
 * - w/t sub-tasks: 'Original Estimate' is set manually.
 * ### Triggers ###
 * - Original Estimation fields update;
 * - any issue is added/removed/reassigned to/from/between Epic/s;
 * - issue created with 'Original Estimate'.
 */

final String EPIC_LINK = "Epic Link"
final String ORIGINAL_ESTIMATE = "timeoriginalestimate"

def customFields = Unirest.get("/rest/api/2/field").asObject(List).body.findAll { (it as Map).custom } as List<Map>
def epicLinkId = customFields.find { it.name == EPIC_LINK }?.id

def issueType = issue.fields.issuetype.name as String
if (issueType == "Epic") return
def isSubTask = issue.fields.issuetype.subtask as Boolean
def originalEstimateChange = changelog?.items?.find { (it as Map).field == ORIGINAL_ESTIMATE } as Map
def epicLinkChange = changelog?.items?.find { (it as Map).field == EPIC_LINK } as Map
def wasCreatedWithOriginalEstimate = issue_event_type_name == "issue_created" && issue.fields[ORIGINAL_ESTIMATE] > 0

def epicOriginalEstimateCalculation = { String epicKey ->
    def jqlIssuesInEpic = """ "${EPIC_LINK}" = ${epicKey} """
    def issuesInEpic = executeSearch(jqlIssuesInEpic, 0, 500) as List
    def epicOriginalEstimateVal = issuesInEpic?.findResults { it.fields[ORIGINAL_ESTIMATE] }?.sum()
    if (epicOriginalEstimateVal) epicOriginalEstimateVal /= 60
    setOriginalEstimate(epicKey, epicOriginalEstimateVal)
}

def parentOriginalEstimateCalculation = { Map parentIssue ->
    def subTasksKeys = parentIssue.fields.subtasks*.key
    def parentOriginalEstimateVal = subTasksKeys.findResults { getIssue(it).fields[ORIGINAL_ESTIMATE] }.sum()
    if (parentOriginalEstimateVal) parentOriginalEstimateVal  /= 60
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

if (isSubTask && originalEstimateChange) {
    def parentIssueKey = issue.fields.parent.key
    def parentIssue = getIssue(parentIssueKey)
    parentOriginalEstimateCalculation(parentIssue)
    def epicIssueKey = parentIssue.fields[epicLinkId]
    if (epicIssueKey) epicOriginalEstimateCalculation(epicIssueKey)
} else if (!isSubTask && (originalEstimateChange || epicLinkChange || wasCreatedWithOriginalEstimate)) {
    def epics = findAffectedEpics(issue)
    epics.each { epicOriginalEstimateCalculation(it) }
}

static Map getIssue(String issueKey) {
    Unirest.get("/rest/api/2/issue/${issueKey}").asObject(Map).body
}

static setOriginalEstimate(issueKey, minutes) {
    Unirest.put("/rest/api/2/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: [timetracking: [originalEstimate: "${minutes}m"]]])
            .asString()
}

static List executeSearch(String jqlQuery, int startAt, int maxResults) {
    def searchRequest = Unirest.get("/rest/api/2/search")
            .queryString("jql", jqlQuery)
            .queryString("startAt", startAt)
            .queryString("maxResults", maxResults)
            .asObject(Map)
    searchRequest.status == 200 ? searchRequest.body.issues as List : null
}