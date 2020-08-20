package jira.listeners.set_field_based_on

/**
 * Current listener sets start/end dates in Epic based on sprint start/end dates related to issues in Epic.
 * Triggers:
 *  adding an issue to Epic;
 *  issue removal from Epic;
 *  any update of sprint value of issues in Epic.
 * The calculation of the Epic:
 *  search for all epic children that are assigned to a closed/active sprint;
 *  get the earliest sprint start date and populate the Start Date field of the epic;
 *  get the latest sprint date and populate the End Date field of the Epic.
 */

import com.atlassian.greenhopper.service.sprint.Sprint
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.event.issue.link.IssueLinkCreatedEvent
import com.atlassian.jira.event.issue.link.IssueLinkDeletedEvent
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue

final String START_DATE_CF_NAME = "Start date"
final String END_DATE_CF_NAME = "End date"
final List<String> ALLOWED_SPRINT_STATUSES = ["ACTIVE", "CLOSED"]
final List<String> ALLOWED_ISSUE_TYPE_NAMES = ["Story", "Bug"]

// indicate event and get mutable issue
def issueKey = null
def triggerEvent = null
if (event instanceof IssueLinkCreatedEvent) {
    triggerEvent = event as IssueLinkCreatedEvent
    issueKey = event.issueLink.sourceObject.key
} else if (event instanceof IssueLinkDeletedEvent) {
    triggerEvent = event as IssueLinkDeletedEvent
    issueKey = event.issueLink.sourceObject.key
} else if (event instanceof IssueEvent) {
    triggerEvent = event as IssueEvent
    // check if sprint was changed and change is related to allowed issue types
    if (!(triggerEvent.issue.issueType.name in ALLOWED_ISSUE_TYPE_NAMES)) return
    def changeLog = triggerEvent?.changeLog
    def changeItems = changeLog.getRelated("ChildChangeItem")
    if (changeItems?.any { it.field.toString().equalsIgnoreCase("sprint")}) {
        def triggerIssue = triggerEvent.issue
        issueKey = ComponentAccessor.issueLinkManager.getInwardLinks(triggerIssue.id).
                find { it.issueLinkType.name == "Epic-Story Link" }.sourceObject.key
    }
}
// condition for running script
if (!issueKey || !triggerEvent) return
def epicIssue = getIssue(issueKey)
if (epicIssue.issueType.name != "Epic") return

// search for all epic children that are assigned to a sprint
def issuesInEpic = getIssuesInEpic(epicIssue)
def epicIssuesWithSprints = issuesInEpic.findAll { issueInEpic ->
    def sprints = getCustomFieldValue("Sprint", issueInEpic) as List
    sprints && !sprints.empty
}
// find all related active/closed sprints
def actualSprints = new ArrayList<Sprint>()
epicIssuesWithSprints.each { issueInEpic ->
    def sprints = getCustomFieldValue("Sprint", issueInEpic) as List<Sprint>
    def activeClosedSprints = sprints.findAll {
        it.state.toString() in ALLOWED_SPRINT_STATUSES
    }
    actualSprints.addAll(activeClosedSprints)
}
// get the earliest sprint start date and populate the Start Date field of the epic
actualSprints.sort { it.startDate }
def earliestSprintStartDate = actualSprints.first().startDate.toDate().toTimestamp()
def startDateCf = ComponentAccessor.customFieldManager.getCustomFieldObjects().find { it.name == START_DATE_CF_NAME }
epicIssue.setCustomFieldValue(startDateCf, earliestSprintStartDate)

// get the latest sprint date and populate the End Date field of the Epic
actualSprints.sort { it.endDate }
def latestSprintEndDate = actualSprints.last().endDate.toDate().toTimestamp()
def endDateCf = ComponentAccessor.customFieldManager.getCustomFieldObjects().find { it.name == END_DATE_CF_NAME }
epicIssue.setCustomFieldValue(endDateCf, latestSprintEndDate)

// epic update
def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
ComponentAccessor.issueManager.updateIssue(currentUser, epicIssue, EventDispatchOption.ISSUE_UPDATED, false)

static MutableIssue getIssue(String issueKey) {
    ComponentAccessor.issueManager.getIssueObject(issueKey)
}

static Object getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
            .find { it.name == customFieldName }?.getValue(issue)
}

static List<Issue> getIssuesInEpic(Issue epic) {
    ComponentAccessor.issueLinkManager.getOutwardLinks(epic.id).
            findAll { it.issueLinkType.name == "Epic-Story Link" }*.destinationObject
}