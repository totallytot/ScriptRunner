package jira.listeners.set_field_based_on

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.event.issue.link.IssueLinkCreatedEvent
import com.atlassian.jira.event.issue.link.IssueLinkDeletedEvent
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue

/**
 * Current listener sets start/end dates in Theme based on start/end dates related to linked Epics.
 * Hierarchy: Theme - Epic - Story/Bug.
 * Epics are linked to Themes as "In Theme", Themes are linked to Epics as "Theme of".
 * The calculation of Theme:
 *  - search for all linked epics of the theme;
 *  - get the earliest Epic’s Start Date and populate the Start Date field of the Theme;
 *  - get the latest Epics End Date and populate the End Date field of the Theme.
 * Triggers:
 *  - any update of start/end dates of linked Epics;
 *  - link creation event between Epic and Theme;
 *  - link removal event between Epic and Theme.
 */

final String START_DATE_CF_NAME = "Start date"
final String END_DATE_CF_NAME = "End date"
final List<String> ALLOWED_ISSUE_TYPE_NAMES = ["Epic"]

// Indicate trigger event and get key for retrieving Theme as mutable issue.
def issueKey = null
def triggerEvent = null
if (event instanceof IssueLinkCreatedEvent) {
    triggerEvent = event as IssueLinkCreatedEvent
    if (triggerEvent.issueLink.issueLinkType.name != "Theme") return
    issueKey = triggerEvent.issueLink.destinationObject.key
} else if (event instanceof IssueLinkDeletedEvent) {
    triggerEvent = event as IssueLinkDeletedEvent
    if (triggerEvent.issueLink.issueLinkType.name != "Theme") return
    issueKey = triggerEvent.issueLink.destinationObject.key
} else if (event instanceof IssueEvent) {
    triggerEvent = event as IssueEvent
    // Check if start/end dates were changed and change is related to allowed issue types.
    if (!(triggerEvent.issue.issueType.name in ALLOWED_ISSUE_TYPE_NAMES)) return
    def changeItems = triggerEvent.changeLog.getRelated("ChildChangeItem")
    def wasStartDateUpdated = changeItems?.any { it.field.toString().equalsIgnoreCase(START_DATE_CF_NAME) }
    def wasEndDateUpdated = changeItems?.any { it.field.toString().equalsIgnoreCase(END_DATE_CF_NAME) }
    if (wasEndDateUpdated || wasStartDateUpdated) {
        def triggerIssue = triggerEvent.issue
        issueKey = ComponentAccessor.issueLinkManager.getOutwardLinks(triggerIssue.id).
                find { it.issueLinkType.name == "Theme" }?.destinationObject?.key
    }
}

// Condition for running script.
if (!issueKey || !triggerEvent) return
def themeIssue = getIssue(issueKey as String) as MutableIssue
if (themeIssue.issueType.name != "Theme") return

// Find Epics in Theme and calculate Epic's earliest start date and latest end date.
def issuesInTheme = getIssuesInTheme(themeIssue).findAll { it.issueType.name in ALLOWED_ISSUE_TYPE_NAMES }
if (!issuesInTheme || issuesInTheme.empty) return
def startDatesValues = issuesInTheme.findResults { getCustomFieldValue(START_DATE_CF_NAME, it) } as List<Date>
def earliestStartDate = startDatesValues.sort().first()
def endDatesValues = issuesInTheme.findResults { getCustomFieldValue(END_DATE_CF_NAME, it) } as List<Date>
def latestEndDate = endDatesValues.sort().last()

// Theme update.
def customFieldManager = ComponentAccessor.customFieldManager
if (earliestStartDate) {
    def startDateCf = customFieldManager.customFieldObjects.find { it.name == START_DATE_CF_NAME }
    themeIssue.setCustomFieldValue(startDateCf, earliestStartDate.toTimestamp())
}
if (latestEndDate) {
    def startDateCf = customFieldManager.customFieldObjects.find { it.name == END_DATE_CF_NAME }
    themeIssue.setCustomFieldValue(startDateCf, latestEndDate.toTimestamp())
}
if (earliestStartDate || latestEndDate) {
    def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
    ComponentAccessor.issueManager.updateIssue(currentUser, themeIssue, EventDispatchOption.ISSUE_UPDATED, false)
}

static MutableIssue getIssue(String issueKey) { ComponentAccessor.issueManager.getIssueObject(issueKey) }

static List<Issue> getIssuesInTheme(Issue theme) {
    ComponentAccessor.issueLinkManager.getInwardLinks(theme.id).
            findAll { it.issueLinkType.name == "Theme" }*.sourceObject
}

static Object getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
            .find { it.name == customFieldName }?.getValue(issue)
}