package jira_cloud.listeners.set_field_value_based_on.hierarchy

import kong.unirest.Unirest

/**
 * ### Hierarchy ###
 *  Story, Bug, Design Story, Dev Story -> Epic -> Initiative -> Theme
 * 'Effort Sizing' is filled in manually in Story, Bug, Design Story, Dev Story. Afterwards, 'Estimated days to complete'
 * is updated by Automation Module based on rule.
 * ### Script logic ###
 * For Epic:
 *  - 'Estimated days to complete' = Sum of Estimated days to complete' of all issues in Epic;
 *  - 'Remaining days to complete' = Epic's 'Estimated days to complete' - 'Estimated days to complete' of resolved
 *  issues in Epic, if there are no resolved issues use 0 instead of null;
 *  - 'Progress' = ('Epic's Estimated days to complete' - Epic's 'Remaining to complete') / Epic's 'Estimated days
 *  to complete' * 100;
 *  - 'All Childs Resolved' = checks if all issues in Epic have resolution.
 * For Initiative:
 *  - 'Estimated days to complete' = Sum of 'Estimated days to complete' of EpicsWithoutIssues in this Initiative;
 *  - 'Remaining days to complete' = Sum 'Remaining days to complete' of EpicsWithoutIssues;
 *  - 'Progress' = ('Initiative's Estimated days to complete' - Initiative's 'Remaining to complete') / Initiative's
 *  'Estimated days to complete' * 100;
 *  - 'All Childs Resolved' = checks if all child issues have resolution.
 * For Theme:
 *  - 'Estimated days to complete' = Sum of 'Estimated days to complete' of initiatives in this theme;
 *  - 'Remaining days to complete' = Sum 'Remaining days to complete' of Initiatives in Theme;
 *  - 'Progress' = ('Theme's Estimated days to complete' - Theme's 'Remaining to complete') / 'Theme's 'Estimated days
 *  to complete' * 100;
 *  - 'All Childs Resolved' = checks if all child issues have resolution.
 * ### Triggers ###
 * - 'Estimated days to complete' is updated;
 * - any issue is added/removed/reassigned to/from/between Epic/s;
 * - any issue is added/removed/reassigned to/from/between Initiative/s;
 * - any issue is added/removed/reassigned to/from/between Theme/s;
 * - resolution change;
 * - Story, Bug, Design Story, Dev Story - issue created and 'Effort Sizing'/'Estimated days to complete' is not empty.
 */

final String EPIC_LINK = "Epic Link"
final String PARENT_LINK = "Parent Link"
final String EFFORT_SIZING = "Effort Sizing"
final String ESTIMATED_DAYS_TO_COMPLETE = "Estimated days to complete"
final String REMAINING_DAYS_TO_COMPLETE = "Remaining days to complete"
final String PROGRESS = "Progress"
final String ALL_CHILDS_RESOLVED = "All Childs Resolved"

def customFields = customFields
def epicLinkId = customFields.find { it.name == EPIC_LINK }?.id
def parentLinkId = customFields.find { it.name == PARENT_LINK }?.id
def estimatedDaysToCompleteId = customFields.find { it.name == ESTIMATED_DAYS_TO_COMPLETE }?.id
def remainingDaysToCompleteId = customFields.find { it.name == REMAINING_DAYS_TO_COMPLETE }?.id
def progressId = customFields.find { it.name == PROGRESS }?.id
def allChildsResolvedId = customFields.find { it.name == ALL_CHILDS_RESOLVED }?.id
def issueType = issue.fields.issuetype.name as String

def calculateEpicIssue = { String issueKey ->
    logger.info "Epic Issue Calculation: ${issueKey} "
    def jqlIssuesInEpic = """ "${EPIC_LINK}" = ${issueKey} """
    def issuesInEpic = executeSearch(jqlIssuesInEpic, 0, 500)
    def epicFieldsValsMapping = [:]

    def allChildsResolvedVal = !issuesInEpic.any { it.fields.resolution == null }
    logger.info "allChildsResolvedVal ${allChildsResolvedVal}"
    epicFieldsValsMapping.put(allChildsResolvedId, allChildsResolvedVal.toString())

    def estimatedDaysToCompleteVal = issuesInEpic.findResults { it.fields[estimatedDaysToCompleteId] }.sum()
    logger.info "estimatedDaysToCompleteVal ${estimatedDaysToCompleteVal}"
    epicFieldsValsMapping.put(estimatedDaysToCompleteId, estimatedDaysToCompleteVal)

    def estimatedDaysToCompleteResolvedIssues = issuesInEpic.findResults {
        it.fields.resolution ? it.fields[estimatedDaysToCompleteId] : 0
    }.sum()
    def remainingDaysToCompleteVal = 0
    if ((estimatedDaysToCompleteVal || estimatedDaysToCompleteVal == 0) &&
            (estimatedDaysToCompleteResolvedIssues || estimatedDaysToCompleteResolvedIssues == 0))
        remainingDaysToCompleteVal = estimatedDaysToCompleteVal - estimatedDaysToCompleteResolvedIssues
    logger.info "remainingDaysToCompleteVal ${remainingDaysToCompleteVal}"
    epicFieldsValsMapping.put(remainingDaysToCompleteId, remainingDaysToCompleteVal)

    int progressVal = (estimatedDaysToCompleteVal - remainingDaysToCompleteVal) / estimatedDaysToCompleteVal * 100
    logger.info "progressVal ${progressVal}"
    epicFieldsValsMapping.put(progressId, progressVal)

    def status = setFields(issueKey, epicFieldsValsMapping)
    logger.info "Status ${status}"
}

def calculateRoadmapIssue = { String issueKey ->
    logger.info "Roadmap Issue Calculation: ${issueKey}"
    def jqlChildIssues = """ "${PARENT_LINK}" = ${issueKey} """
    def childIssues = executeSearch(jqlChildIssues, 0, 500)
    def roadmapIssueFieldsValsMapping = [:]

    def allChildsResolvedVal = !childIssues.any { it.fields.resolution == null }
    logger.info "allChildsResolvedVal ${allChildsResolvedVal}"
    roadmapIssueFieldsValsMapping.put(allChildsResolvedId, allChildsResolvedVal.toString())

    def estimatedDaysToCompleteVal = childIssues.findResults { it.fields[estimatedDaysToCompleteId] }.sum()
    logger.info "estimatedDaysToCompleteVal ${estimatedDaysToCompleteVal}"
    roadmapIssueFieldsValsMapping.put(estimatedDaysToCompleteId, estimatedDaysToCompleteVal)

    def remainingDaysToCompleteVal = childIssues.findResults { it.fields[remainingDaysToCompleteId] }.sum()
    logger.info "remaningDaysToComplete ${remainingDaysToCompleteVal}"
    roadmapIssueFieldsValsMapping.put(remainingDaysToCompleteId, remainingDaysToCompleteVal)

    int progressVal = (estimatedDaysToCompleteVal - remainingDaysToCompleteVal) / estimatedDaysToCompleteVal * 100
    logger.info "progressVal ${progressVal}"
    roadmapIssueFieldsValsMapping.put(progressId, progressVal)

    def status = setFields(issueKey, roadmapIssueFieldsValsMapping)
    logger.info "Status ${status}"
}

logger.info "${issue_event_type_name} ${issueType} ${issue.key}"
switch (issueType) {
    case "Story":
    case "Bug":
    case "Design Story":
    case "Dev Story":
        def shouldRun = changelog.items.field.any {
            it.toString() in [ESTIMATED_DAYS_TO_COMPLETE, EPIC_LINK, "resolution"]
        } || (issue_event_type_name == "issue_created" &&
                (issue.fields[EFFORT_SIZING] || issue.fields[ESTIMATED_DAYS_TO_COMPLETE]))
        logger.info "shouldRun ${shouldRun}"
        if (!shouldRun) return
        def epicKeys = []
        def epicLinkChange = changelog?.items?.find { (it as Map).field == EPIC_LINK } as Map
        if (epicLinkChange) {
            epicKeys << epicLinkChange.fromString
            epicKeys << epicLinkChange.toString
        }
        epicKeys.removeAll { it == null }
        if (epicKeys.empty) epicKeys << issue.fields[epicLinkId]
        epicKeys.each { String epicKey ->
            calculateEpicIssue(epicKey)
            def initiativeKey = getIssue(epicKey).fields[parentLinkId].data.key
            logger.info "Initiative ${initiativeKey}"
            if (!initiativeKey) return
            calculateRoadmapIssue(initiativeKey)
            def themeKey = getIssue(initiativeKey).fields[parentLinkId].data.key
            logger.info "Theme ${themeKey}"
            if (!themeKey) return
            calculateRoadmapIssue(themeKey)
        }
        break
    case "Epic":
        def shouldRun = changelog.items.field.any { it.toString() in [PARENT_LINK, "resolution"] }
        logger.info "shouldRun ${shouldRun}"
        if (!shouldRun) return
        def initiativeKeys = []
        def parentLinkChange = changelog.items.find { (it as Map).field == PARENT_LINK } as Map
        if (parentLinkChange) {
            initiativeKeys << parentLinkChange.fromString
            initiativeKeys << parentLinkChange.toString
        }
        initiativeKeys.removeAll { it == null }
        if (initiativeKeys.empty) initiativeKeys << issue.fields[parentLinkId].data.key
        initiativeKeys.each { String initiativeKey ->
            calculateRoadmapIssue(initiativeKey)
            def themeKey = getIssue(initiativeKey).fields[parentLinkId].data.key
            logger.info "Theme ${themeKey}"
            if (!themeKey) return
            calculateRoadmapIssue(themeKey)
        }
        break
    case "Initiative":
        def shouldRun = changelog.items.field.any { it.toString() in [PARENT_LINK, "resolution"] }
        logger.info "shouldRun ${shouldRun}"
        if (!shouldRun) return
        def themeKeys = []
        def parentLinkChange = changelog.items.find { (it as Map).field == PARENT_LINK } as Map
        if (parentLinkChange) {
            themeKeys << parentLinkChange.fromString
            themeKeys << parentLinkChange.toString
        }
        themeKeys.removeAll { it == null }
        if (themeKeys.empty) themeKeys << issue.fields[parentLinkId].data.key
        themeKeys.each { calculateRoadmapIssue(it) }
        break
}

static Map getIssue(String issueKey) {
    Unirest.get("/rest/api/2/issue/${issueKey}")
            .header('Content-Type', 'application/json')
            .asObject(Map).body
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

static List<Map> getCustomFields() {
    Unirest.get("/rest/api/2/field")
            .header('Content-Type', 'application/json')
            .asObject(List)
            .body
            .findAll { (it as Map).custom } as List<Map>
}