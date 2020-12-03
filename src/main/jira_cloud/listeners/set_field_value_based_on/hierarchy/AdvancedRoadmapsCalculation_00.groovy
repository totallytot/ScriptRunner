package jira_cloud.listeners.set_field_value_based_on.hierarchy

import kong.unirest.Unirest

/**
 * Hierarchy:
 * Story, Bug, Design Story, Dev Story -> Epic ->  Initiative -> Theme
 * 'Effort Sizing' is filled in manually in Story, Bug, Design Story, Dev Story.
 *
 * Story, Bug, Design Story, Dev Story calculation:
 *  - If 'Effort Sizing' = Small Effort, then 'Estimated days to complete' =  0.5
 *  - If 'Effort Sizing' = Medium Effort, then 'Estimated days to complete' =  1
 *  - If 'Effort Sizing' = Large Effort, then 'Estimated days to complete' =  3
 *  - If 'Effort Sizing' = XL Effort, then 'Estimated days to complete' =  5
 * Epic:
 *  - 'Estimated days to complete' = Sum of Estimated days to complete' of all issues in epic
 * Initiative:
 *  - 'Estimated days to complete' = Sum of 'Estimated days to complete' of epicsWithoutIssues in this initiative.
 * Theme:
 *  - 'Estimated days to complete' = Sum of 'Estimated days to complete' of initiatives in this theme.
 *
 *  Triggers:
 * - 'Effort Sizing' is updated
 * - any issue is added/removed/reassigned to/from/between Epic/s
 * - any issue is added/removed/reassigned to/from/between Initiative/s
 * - any issue is added/removed/reassigned to/from/between Theme/s
 */

final String EPIC_LINK = "Epic Link"
final String PARENT_LINK = "Parent Link"
final String EFFORT_SIZING = "Effort Sizing"
final String ESTIMATED_DAYS_TO_COMPLETE = "Estimated days to complete"
final REGULAR_ISSUE_TYPES = ["Story", "Bug", "Design Story", "Dev Story"]

def customFields = Unirest.get("/rest/api/2/field").asObject(List).body.findAll { (it as Map).custom } as List<Map>
def epicLinkId = customFields.find { it.name == EPIC_LINK }?.id
def parentLinkId = customFields.find { it.name == PARENT_LINK }?.id
def effortSizingId = customFields.find { it.name == EFFORT_SIZING }?.id
def estimatedDaysToCompleteId = customFields.find { it.name == ESTIMATED_DAYS_TO_COMPLETE }?.id

def issueType = issue.fields.issuetype.name as String

def calculateRegularIssue = { String issueKey ->
    logger.info "Calculate Regular Issue ${issueKey}"
    def estimatedDaysToCompleteVal = null
    def effortSizingVal = issue.fields[effortSizingId].value
    switch (effortSizingVal) {
        case "Small Effort":
            estimatedDaysToCompleteVal = 0.5
            break
        case "Medium Effort":
            estimatedDaysToCompleteVal = 1
            break
        case "Large Effort":
            estimatedDaysToCompleteVal = 3
            break
        case "XL Effort":
            estimatedDaysToCompleteVal = 5
            break
    }
    def status = setField(issueKey, estimatedDaysToCompleteId, estimatedDaysToCompleteVal)
    logger.info "Status ${status} estimatedDaysToCompleteVal ${estimatedDaysToCompleteVal}"
}

def calculateEpicIssue = { String issueKey ->
    logger.info "Calculate Epic Issue ${issueKey}"
    def jqlIssuesInEpic = """ "${EPIC_LINK}" = ${issueKey} """
    def issuesInEpic = executeSearch(jqlIssuesInEpic, 0, 500)
    def estimatedDaysToCompleteVal = issuesInEpic.findResults { it.fields[estimatedDaysToCompleteId] }.sum()
    def status = setField(issueKey, estimatedDaysToCompleteId, estimatedDaysToCompleteVal)
    logger.info "Status ${status} estimatedDaysToCompleteVal ${estimatedDaysToCompleteVal}"
}

def calculateRoadmapIssue = { String issueKey ->
    logger.info "Calculate Roadmap Issue ${issueKey}"
    def jqlIssuesInParent = """ "${PARENT_LINK}" = ${issueKey} """
    def issuesInEpic = executeSearch(jqlIssuesInParent, 0, 500)
    def estimatedDaysToCompleteVal = issuesInEpic.findResults { it.fields[estimatedDaysToCompleteId] }.sum()
    def status = setField(issueKey, estimatedDaysToCompleteId, estimatedDaysToCompleteVal)
    logger.info "Status ${status} estimatedDaysToCompleteVal ${estimatedDaysToCompleteVal}"
}

if (issueType in REGULAR_ISSUE_TYPES && issue_event_type_name == "issue_created") {
    calculateRegularIssue(issue.key)
    def epicKey = issue.fields[epicLinkId]
    if (!epicKey) return
    calculateEpicIssue(epicKey)
    def initiativeKey = getIssue(epicKey).fields[parentLinkId].data?.key
    logger.info "Initiative ${initiativeKey}"
    if (!initiativeKey) return
    calculateRoadmapIssue(initiativeKey)
    def themeKey = getIssue(initiativeKey).fields[parentLinkId].data?.key
    logger.info "Theme ${themeKey}"
    if (!themeKey) return
    calculateRoadmapIssue(themeKey)
} else if (issueType in REGULAR_ISSUE_TYPES && issue_event_type_name == "issue_updated") {
    def shouldRun = changelog.items.field.any { it.toString() in [EFFORT_SIZING, EPIC_LINK] }
    logger.info "shouldRun ${shouldRun}"
    if (!shouldRun) return
    def epicKeys = []
    epicKeys << issue.fields[epicLinkId]
    def epicLinkChange = changelog.items.find { (it as Map).field == EPIC_LINK } as Map
    if (epicLinkChange) {
        epicKeys << epicLinkChange.fromString
        epicKeys << epicLinkChange.toString
    }
    epicKeys.removeAll { it == null }
    epicKeys.each { String epicKey ->
        calculateEpicIssue(epicKey)
        def initiativeKey = getIssue(epicKey).fields[parentLinkId].data?.key
        logger.info "Initiative ${initiativeKey}"
        if (!initiativeKey) return
        calculateRoadmapIssue(initiativeKey)
        def themeKey = getIssue(initiativeKey).fields[parentLinkId].data?.key
        logger.info "Theme ${themeKey}"
        if (!themeKey) return
        calculateRoadmapIssue(themeKey)
    }
} else if (issueType == "Epic" && issue_event_type_name == "issue_updated") {
    def shouldRun = changelog.items.field.any { it.toString() in [PARENT_LINK] }
    if (!shouldRun) return
    def initiativeKeys = []
    def parentLinkChange = changelog.items.find { (it as Map).field == PARENT_LINK } as Map
    if (parentLinkChange) {
        initiativeKeys << parentLinkChange.fromString
        initiativeKeys << parentLinkChange.toString
    }
    initiativeKeys.removeAll { it == null }
    initiativeKeys.each { String initiativeKey ->
        calculateRoadmapIssue(initiativeKey)
        def themeKey = getIssue(initiativeKey).fields[parentLinkId].data?.key
        logger.info "Theme ${themeKey}"
        if (!themeKey) return
        calculateRoadmapIssue(themeKey)
    }
} else if (issueType == "Initiative" && issue_event_type_name == "issue_updated") {
    def shouldRun = changelog.items.field.any { it.toString() in [PARENT_LINK] }
    if (!shouldRun) return
    def themeKeys = []
    def parentLinkChange = changelog.items.find { (it as Map).field == PARENT_LINK } as Map
    if (parentLinkChange) {
        themeKeys << parentLinkChange.fromString
        themeKeys << parentLinkChange.toString
    }
    themeKeys.removeAll { it == null }
    themeKeys.each { calculateRoadmapIssue(it) }
}

static int setField(String issueKey, String customfield_id, value) {
    def result = Unirest.put("/rest/api/2/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: [(customfield_id): value]]).asString()
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

static Map getIssue(String issueKey) {
    Unirest.get("/rest/api/2/issue/${issueKey}").asObject(Map).body
}