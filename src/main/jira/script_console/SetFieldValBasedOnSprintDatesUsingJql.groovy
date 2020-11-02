package jira.script_console

import com.atlassian.greenhopper.service.sprint.Sprint
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import org.apache.log4j.Level
import org.apache.log4j.Logger

final String JQL = "project = TEST"
final String START_DATE_CF_NAME = "Start date"
final String END_DATE_CF_NAME = "End date"
final List<String> ALLOWED_SPRINT_STATUSES = ["ACTIVE", "CLOSED"]
final List<String> ALLOWED_ISSUE_TYPES_IN_EPIC = ["Story", "Bug"]

def logger = Logger.getLogger("check-me")
logger.setLevel(Level.DEBUG)
def customFieldManager = ComponentAccessor.customFieldManager
def issueManager = ComponentAccessor.issueManager
def issueLinkManager = ComponentAccessor.issueLinkManager
def startDateCf = customFieldManager.customFieldObjects.find { it.name == START_DATE_CF_NAME }
def endDateCf = customFieldManager.customFieldObjects.find { it.name == END_DATE_CF_NAME }
def executionUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser

def themeStartEndDatesUpdate = { Issue themeIssue ->
    def issuesInTheme = issueLinkManager.getInwardLinks(themeIssue.id).
            findAll { it.issueLinkType.name == "Theme" }*.sourceObject
    def epicsInTheme = issuesInTheme.findAll { it.issueType.name == "Epic" }
    if (epicsInTheme.empty) return false
    def mutableThemeIssue = issueManager.getIssueObject(themeIssue.key)

    def startDatesValues = epicsInTheme.findResults { getCustomFieldValue(START_DATE_CF_NAME, it) } as List<Date>
    def earliestStartDate = startDatesValues.sort().first()
    logger.debug "Theme ${mutableThemeIssue.key} earliest start date: ${earliestStartDate}"
    mutableThemeIssue.setCustomFieldValue(startDateCf, earliestStartDate.toTimestamp())

    def endDatesValues = epicsInTheme.findResults { getCustomFieldValue(END_DATE_CF_NAME, it) } as List<Date>
    def latestEndDate = endDatesValues.sort().last()
    logger.debug "Theme ${mutableThemeIssue.key} latest end date: ${latestEndDate}"
    mutableThemeIssue.setCustomFieldValue(endDateCf, latestEndDate.toTimestamp())

    if (earliestStartDate || latestEndDate) {
        issueManager.updateIssue(executionUser, mutableThemeIssue, EventDispatchOption.DO_NOT_DISPATCH, false)
        return true
    } else false
}

def epicStartEndDatesUpdate = { Issue epicIssue ->
    def issuesInEpic = issueLinkManager.getOutwardLinks(epicIssue.id).
            findAll { it.issueLinkType.name == "Epic-Story Link" }*.destinationObject

    def allowedIssuesWithSprints = issuesInEpic.findAll { issueInEpic ->
        def sprints = getCustomFieldValue("Sprint", issueInEpic) as List
        issueInEpic.issueType.name in ALLOWED_ISSUE_TYPES_IN_EPIC && sprints && !sprints.empty
    }
    def actualSprints = new ArrayList<Sprint>()
    allowedIssuesWithSprints.each { issueInEpic ->
        def sprints = getCustomFieldValue("Sprint", issueInEpic) as List<Sprint>
        def activeClosedSprints = sprints.findAll { it.state.toString() in ALLOWED_SPRINT_STATUSES }
        actualSprints.addAll(activeClosedSprints)
    }
    if (actualSprints.empty) return false
    logger.debug "Actual Sprints: ${actualSprints*.name}"
    def mutableEpicIssue = issueManager.getIssueObject(epicIssue.key)

    actualSprints.sort { it.startDate }
    def earliestSprintStartDate = actualSprints.first().startDate.toDate()
    logger.debug "Epic ${mutableEpicIssue.key} earliest start date: ${earliestSprintStartDate}"
    mutableEpicIssue.setCustomFieldValue(startDateCf, earliestSprintStartDate.toTimestamp())

    def completeAndEndDates = actualSprints.findResults { Sprint sprint ->
        if (!sprint.completeDate) sprint.endDate
        else sprint.completeDate
    }
    def latestSprintEndDate = completeAndEndDates.sort().last().toDate()
    logger.debug "Epic ${mutableEpicIssue.key} latest end date: ${latestSprintEndDate}"
    mutableEpicIssue.setCustomFieldValue(endDateCf, latestSprintEndDate.toTimestamp())
    if (earliestSprintStartDate || latestSprintEndDate) {
        issueManager.updateIssue(executionUser, mutableEpicIssue, EventDispatchOption.DO_NOT_DISPATCH, false)
        return true
    } else
        return false
}

def storyStartEndDatesUpdate = { Issue storyIssue ->
    def sprints = getCustomFieldValue("Sprint", storyIssue) as List<Sprint>
    if (!sprints || sprints.empty) return false
    def actualSprints = sprints.findAll { it.state.toString() in ALLOWED_SPRINT_STATUSES }
    if (actualSprints.empty) return false
    def mutableStoryIssue = issueManager.getIssueObject(storyIssue.key)

    actualSprints.sort { it.startDate }
    def earliestSprintStartDate = actualSprints.first().startDate.toDate()
    logger.debug "Story ${mutableStoryIssue.key} earliest start date: ${earliestSprintStartDate}"
    if (earliestSprintStartDate) mutableStoryIssue.setCustomFieldValue(startDateCf, earliestSprintStartDate.toTimestamp())

    def completeAndEndDates = actualSprints.findResults { Sprint sprint ->
        if (!sprint.completeDate) sprint.endDate
        else sprint.completeDate
    }
    def latestSprintEndDate = completeAndEndDates.sort().last().toDate()
    logger.debug "Story ${mutableStoryIssue.key} latest end date: ${latestSprintEndDate}"
    if (latestSprintEndDate) mutableStoryIssue.setCustomFieldValue(endDateCf, latestSprintEndDate.toTimestamp())
    if (earliestSprintStartDate || latestSprintEndDate) {
        issueManager.updateIssue(executionUser, mutableStoryIssue, EventDispatchOption.DO_NOT_DISPATCH, false)
        return true
    } else return false
}

def affectedIssues = getIssuesFromJql(executionUser, JQL)
if (!affectedIssues && !affectedIssues.empty) return

affectedIssues.findAll { it.issueType.name == "Story" }.each { storyStartEndDatesUpdate(it) }
affectedIssues.findAll { it.issueType.name == "Epic" }.each { epicStartEndDatesUpdate(it) }
affectedIssues.findAll { it.issueType.name == "Theme" }.each { themeStartEndDatesUpdate(it) }

static Object getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
            .find { it.name == customFieldName }?.getValue(issue)
}

static List<Issue> getIssuesFromJql(ApplicationUser executionUser, String jql) {
    def searchService = ComponentAccessor.getComponentOfType(SearchService)
    def parseResult = searchService.parseQuery(executionUser, jql)
    if (parseResult.valid)
        searchService.search(executionUser, parseResult.query, PagerFilter.unlimitedFilter).results
    else null
}