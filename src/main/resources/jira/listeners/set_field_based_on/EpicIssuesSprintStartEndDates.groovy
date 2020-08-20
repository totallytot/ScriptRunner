package jira.listeners.set_field_based_on

import com.atlassian.greenhopper.service.sprint.Sprint
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.event.issue.link.IssueLinkCreatedEvent
import com.atlassian.jira.event.issue.link.IssueLinkDeletedEvent
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue

/**
 * Current listener sets start/end dates in Epic based on sprint start/end dates related to issues
 * in Epic (story and bug only). Hierarchy: Theme - Epic - Story/Bug.
 * The calculation of the Epic:
 *  - search for all epic children that are assigned to a closed/active sprint;
 *  - get the earliest sprint start date and populate the Start Date field of the epic;
 *  - get the latest sprint date and populate the End Date field of the Epic.
 * Triggers:
 *  - adding an issue to Epic;
 *  - issue removal from Epic;
 *  - any update of sprint value of issues in Epic.
 */

final String START_DATE_CF_NAME = "Start date"
final String END_DATE_CF_NAME = "End date"
final List<String> ALLOWED_SPRINT_STATUSES = ["ACTIVE", "CLOSED"]
final List<String> ALLOWED_ISSUE_TYPE_NAMES = ["Story", "Bug"]

// Indicate trigger event and get key for retrieving Epic as mutable issue.
def issueKey = null
def triggerEvent = null
if (event instanceof IssueLinkCreatedEvent) {
    triggerEvent = event as IssueLinkCreatedEvent
    issueKey = triggerEvent.issueLink.sourceObject.key
} else if (event instanceof IssueLinkDeletedEvent) {
    triggerEvent = event as IssueLinkDeletedEvent
    issueKey = triggerEvent.issueLink.sourceObject.key
} else if (event instanceof IssueEvent) {
    triggerEvent = event as IssueEvent
    // Check if sprint was changed and change is related to allowed issue types.
    if (!(triggerEvent.issue.issueType.name in ALLOWED_ISSUE_TYPE_NAMES)) return
    def changeItems = triggerEvent.changeLog.getRelated("ChildChangeItem")
    if (changeItems?.any { it.field.toString().equalsIgnoreCase("sprint") }) {
        def triggerIssue = triggerEvent.issue
        issueKey = ComponentAccessor.issueLinkManager.getInwardLinks(triggerIssue.id).
                find { it.issueLinkType.name == "Epic-Story Link" }?.sourceObject?.key
    }
}

// Condition for running script.
if (!issueKey || !triggerEvent) return
def epicIssue = getIssue(issueKey as String) as MutableIssue
if (epicIssue.issueType.name != "Epic") return

// Search for allowed Epic's children, that are assigned to sprints.
def issuesInEpic = getIssuesInEpic(epicIssue).findAll { it.issueType.name in ALLOWED_ISSUE_TYPE_NAMES }
def epicIssuesWithSprints = issuesInEpic.findAll { issueInEpic ->
    def sprints = getCustomFieldValue("Sprint", issueInEpic) as List
    sprints && !sprints.empty
}

// Find all related active/closed sprints.
def actualSprints = new ArrayList<Sprint>()
epicIssuesWithSprints.each { issueInEpic ->
    def sprints = getCustomFieldValue("Sprint", issueInEpic) as List<Sprint>
    def activeClosedSprints = sprints.findAll { it.state.toString() in ALLOWED_SPRINT_STATUSES }
    actualSprints.addAll(activeClosedSprints)
}
if (actualSprints.empty) return

// Get the earliest sprint start date.
actualSprints.sort { it.startDate }
def earliestSprintStartDate = actualSprints.first().startDate.toDate()

// Get the latest sprint end date.
actualSprints.sort { it.endDate }
def latestSprintEndDate = actualSprints.last().endDate.toDate()

// Epic update.
def customFieldManager = ComponentAccessor.customFieldManager
if (earliestSprintStartDate) {
    def startDateCf = customFieldManager.customFieldObjects.find { it.name == START_DATE_CF_NAME }
    epicIssue.setCustomFieldValue(startDateCf, earliestSprintStartDate.toTimestamp())
}
if (latestSprintEndDate) {
    def endDateCf = customFieldManager.customFieldObjects.find { it.name == END_DATE_CF_NAME }
    epicIssue.setCustomFieldValue(endDateCf, latestSprintEndDate.toTimestamp())
}
if (earliestSprintStartDate || latestSprintEndDate) {
    def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
    ComponentAccessor.issueManager.updateIssue(currentUser, epicIssue, EventDispatchOption.ISSUE_UPDATED, false)
}

static MutableIssue getIssue(String issueKey) { ComponentAccessor.issueManager.getIssueObject(issueKey) }

static Object getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
            .find { it.name == customFieldName }?.getValue(issue)
}

static List<Issue> getIssuesInEpic(Issue epic) {
    ComponentAccessor.issueLinkManager.getOutwardLinks(epic.id).
            findAll { it.issueLinkType.name == "Epic-Story Link" }*.destinationObject
}