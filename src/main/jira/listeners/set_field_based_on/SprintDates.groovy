package jira.listeners.set_field_based_on

import com.atlassian.greenhopper.api.events.sprint.SprintClosedEvent
import com.atlassian.greenhopper.api.events.sprint.SprintStartedEvent
import com.atlassian.greenhopper.api.events.sprint.SprintUpdatedEvent
import com.atlassian.greenhopper.service.sprint.Sprint
import com.atlassian.greenhopper.service.sprint.SprintIssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.event.issue.link.IssueLinkCreatedEvent
import com.atlassian.jira.event.issue.link.IssueLinkDeletedEvent
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.onresolve.scriptrunner.runner.customisers.JiraAgileBean
import org.apache.log4j.Level
import org.apache.log4j.Logger

/**
 * Hierarchy: Theme - Epic - Story/Bug.
 * Current listener sets:
 *  - start/end dates in Theme based on start/end dates related to linked EpicsWithoutIssues;
 *  - start/end dates in Epic based on sprint start/end dates related to issues in Epic (story and bug only).
 *
 * Epic calculation:
 *  - search for all epic children that are assigned to a closed/active sprint;
 *  - get the earliest sprint start date and populate the Start Date field of the epic;
 *  - get the latest sprint date and populate the End Date field of the Epic.
 * Triggers:
 *  - adding an issue to Epic;
 *  - issue removal from Epic;
 *  - any update of sprint value of issues in Epic;
 *  - sprint started, closed, updated.
 *
 * Theme calculation:
 *  - search for all linked epicsWithoutIssues of the theme;
 *  - get the earliest Epic’s Start Date and populate the Start Date field of the Theme;
 *  - get the latest EpicsWithoutIssues End Date and populate the End Date field of the Theme.
 *  Triggers:
 *  - link creation event between Epic and Theme;
 *  - link removal event between Epic and Theme;
 *  - sprint started, closed, updated.
 *
 *  Story calculation:
 *  - get the earliest sprint start date and populate the Start Date field of the Story;
 *  - get the latest sprint date and populate the End Date field of the Story.
 * Triggers:
 *  - sprint update in Story;
 *  - sprint started, closed, updated.
 *
 *  EventDispatchOption is set to DO_NOT_DISPATCH due to the installed Big Picture app (it updates dates too)
 *
 */

@WithPlugin("com.pyxis.greenhopper.jira")

@JiraAgileBean
SprintIssueService sprintIssueService

def logger = Logger.getLogger("check-me")
logger.setLevel(Level.DEBUG)
logger.debug "!!! Listener Start !!!"

final String START_DATE_CF_NAME = "Start date"
final String END_DATE_CF_NAME = "End date"
final List<String> ALLOWED_SPRINT_STATUSES = ["ACTIVE", "CLOSED"]
final List<String> ALLOWED_ISSUE_TYPES_IN_EPIC = ["Story", "Bug"]

def customFieldManager = ComponentAccessor.customFieldManager
def issueManager = ComponentAccessor.issueManager
def issueLinkManager = ComponentAccessor.issueLinkManager
def startDateCf = customFieldManager.customFieldObjects.find { it.name == START_DATE_CF_NAME }
def endDateCf = customFieldManager.customFieldObjects.find { it.name == END_DATE_CF_NAME }
def executionUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser

// Indicate trigger event
def triggerIssueLink
def triggerSprint
Issue triggerIssue = null
if (event instanceof IssueLinkCreatedEvent) {
    def triggerEvent = event as IssueLinkCreatedEvent
    triggerIssueLink = triggerEvent.issueLink
    logger.debug "Issue Link Created Event"
} else if (event instanceof IssueLinkDeletedEvent) {
    def triggerEvent = event as IssueLinkDeletedEvent
    triggerIssueLink = triggerEvent.issueLink
    logger.debug "Issue Link Deleted Event"
} else if (event instanceof IssueEvent) {
    logger.debug "Issue Updated Event"
    def triggerEvent = event as IssueEvent
    triggerIssue = triggerEvent.issue as Issue
} else if (event instanceof SprintUpdatedEvent) {
    logger.debug "Sprint Updated Event"
    def triggerEvent = event as SprintUpdatedEvent
    triggerSprint = triggerEvent.sprint
} else if (event instanceof SprintStartedEvent) {
    logger.debug "Sprint Started Event"
    def triggerEvent = event as SprintStartedEvent
    triggerSprint = triggerEvent.sprint
} else if (event instanceof SprintClosedEvent) {
    logger.debug "Sprint Closed Event"
    def triggerEvent = event as SprintClosedEvent
    triggerSprint = triggerEvent.sprint
}

def themeStartEndDatesUpdate = { Issue themeIssue ->
    def issuesInTheme = issueLinkManager.getInwardLinks(themeIssue.id).
            findAll { it.issueLinkType.name == "Theme" }*.sourceObject
    def epicsInTheme = issuesInTheme.findAll { it.issueType.name == "Epic" }
    if (epicsInTheme.empty) return false
    def mutableThemeIssue = issueManager.getIssueObject(themeIssue.key)

    def startDatesValues = epicsInTheme.findResults { getCustomFieldValue(START_DATE_CF_NAME, it) } as List<Date>
    def earliestStartDate = startDatesValues.sort().first()
    logger.debug "Theme earliest start date: ${earliestStartDate}"
    mutableThemeIssue.setCustomFieldValue(startDateCf, earliestStartDate.toTimestamp())

    def endDatesValues = epicsInTheme.findResults { getCustomFieldValue(END_DATE_CF_NAME, it) } as List<Date>
    def latestEndDate = endDatesValues.sort().last()
    logger.debug "Theme latest end date: ${latestEndDate}"
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
    logger.debug "Epic earliest start date: ${earliestSprintStartDate}"
    mutableEpicIssue.setCustomFieldValue(startDateCf, earliestSprintStartDate.toTimestamp())

    def completeAndEndDates = actualSprints.findResults { Sprint sprint ->
        if (!sprint.completeDate) sprint.endDate
        else sprint.completeDate
    }
    def latestSprintEndDate = completeAndEndDates.sort().last().toDate()
    logger.debug "Epic latest end date: ${latestSprintEndDate}"
    mutableEpicIssue.setCustomFieldValue(endDateCf, latestSprintEndDate.toTimestamp())
    if (earliestSprintStartDate || latestSprintEndDate) {
        issueManager.updateIssue(executionUser, mutableEpicIssue, EventDispatchOption.DO_NOT_DISPATCH, false)
        return true
    } else
        return false
}

def storyStartEndDatesUpdate = { Issue storyIssue ->
    def sprints = getCustomFieldValue("Sprint", storyIssue) as List<Sprint>
    if (sprints.empty) return false
    def actualSprints = sprints.findAll { it.state.toString() in ALLOWED_SPRINT_STATUSES }
    if (actualSprints.empty) return false
    def mutableStoryIssue = issueManager.getIssueObject(storyIssue.key)

    actualSprints.sort { it.startDate }
    def earliestSprintStartDate = actualSprints.first().startDate.toDate()
    logger.debug "Story earliest start date: ${earliestSprintStartDate}"
    if (earliestSprintStartDate) mutableStoryIssue.setCustomFieldValue(startDateCf, earliestSprintStartDate.toTimestamp())

    def completeAndEndDates = actualSprints.findResults { Sprint sprint ->
        if (!sprint.completeDate) sprint.endDate
        else sprint.completeDate
    }
    def latestSprintEndDate = completeAndEndDates.sort().last().toDate()
    logger.debug "Story latest end date: ${latestSprintEndDate}"
    if (latestSprintEndDate) mutableStoryIssue.setCustomFieldValue(endDateCf, latestSprintEndDate.toTimestamp())
    if (earliestSprintStartDate || latestSprintEndDate) {
        issueManager.updateIssue(executionUser, mutableStoryIssue, EventDispatchOption.DO_NOT_DISPATCH, false)
        return true
    } else return false
}

//do calculation depending on the indicated trigger
if (triggerIssueLink) {
    def linkName = triggerIssueLink.issueLinkType.name
    logger.debug "Triggered by issue link update: ${linkName}"
    switch (linkName) {
        case "Theme":
            def themeIssue = triggerIssueLink.destinationObject
            logger.debug "Theme key: ${themeIssue.key}"
            themeStartEndDatesUpdate(themeIssue)
            break
        case "Epic-Story Link":
            def epicIssue = triggerIssueLink.sourceObject
            logger.debug "Epic key: ${epicIssue.key}"
            epicStartEndDatesUpdate(epicIssue)
            // recalculate and update Theme start/end dates
            def themeIssue = issueLinkManager.getOutwardLinks(epicIssue.id).
                    find { it.issueLinkType.name == "Theme" }?.destinationObject
            if (themeIssue) {
                logger.debug "Theme key from Epic: ${themeIssue.key}"
                themeStartEndDatesUpdate(themeIssue)
            }
            break
    }
} else if (triggerIssue) {
    if (!(triggerIssue.issueType.name in ALLOWED_ISSUE_TYPES_IN_EPIC)) return
    def issueEvent = event as IssueEvent
    def changeItems = issueEvent.changeLog.getRelated("ChildChangeItem")
    if (!changeItems?.any { it.field.toString().equalsIgnoreCase("sprint") }) return
    logger.debug "Triggered by issue sprint update in ${issueEvent.issue.key}"
    // recalculate and update Story start/end dates
    if (triggerIssue.issueType.name == "Story") storyStartEndDatesUpdate(triggerIssue)
    // recalculate and update Epic start/end dates
    def epicIssue = issueLinkManager.getInwardLinks(issueEvent.issue.id).
            find { it.issueLinkType.name == "Epic-Story Link" }?.sourceObject
    if (!epicIssue) return
    logger.debug "Epic is ${epicIssue.key}"
    def wasEpicUpdated = epicStartEndDatesUpdate(epicIssue)
    logger.debug "Was Epic updated ${wasEpicUpdated}"
    // recalculate and update Theme start/end dates
    def themeIssue = issueLinkManager.getOutwardLinks(epicIssue.id).
            find { it.issueLinkType.name == "Theme" }?.destinationObject
    if (!themeIssue) return
    logger.debug "Theme is ${themeIssue.key}"
    if (wasEpicUpdated) {
        def wasThemeUpdated = themeStartEndDatesUpdate(themeIssue)
        logger.debug "Was Theme updated ${wasThemeUpdated}"
    }
} else if (triggerSprint) {
    if (!(triggerSprint.state.name() in ALLOWED_SPRINT_STATUSES)) return
    logger.debug "Trigger sprint is ${triggerSprint.name}"
    //noinspection GroovyVariableNotAssigned
    def sprintIssues = sprintIssueService.getIssuesForSprint(executionUser, triggerSprint).value
    if (sprintIssues.empty) return
    def allowedSprintIssues = sprintIssues.findAll {  Issue issue ->
        issue.issueType.name in ALLOWED_ISSUE_TYPES_IN_EPIC
    } as List<Issue>
    //update of all story issues related to the sprint
    def stories = allowedSprintIssues.findAll { it.issueType.name == "Story" }
    if (!stories.empty) {
        stories.each { Issue story ->
            storyStartEndDatesUpdate(story)
        }
    }
    //update of all epic issues related to the sprint
    def epics = allowedSprintIssues.findResults { Issue issue ->
        issueLinkManager.getInwardLinks(issue.id).
                find { it.issueLinkType.name == "Epic-Story Link" }?.sourceObject
    }
    if (!epics.empty) {
        logger.debug "EpicsWithoutIssues from sprint ${epics*.key}"
        epics.each { Issue epicIssue ->
            def wasEpicUpdated = epicStartEndDatesUpdate(epicIssue)
            if (wasEpicUpdated) {
                def themeIssue = issueLinkManager.getOutwardLinks(epicIssue.id).
                        find { it.issueLinkType.name == "Theme" }?.destinationObject
                if (themeIssue) themeStartEndDatesUpdate(themeIssue)
            }
        }
    }
}

static Object getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
            .find { it.name == customFieldName }?.getValue(issue)
}