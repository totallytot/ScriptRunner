package jira.escalation_services

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.label.LabelManager

def archLabel = "archived_hide_from_tcc_board"

static updateIssueLabels(ApplicationUser executionUser, String label, Issue issue) {
    def labelManager = ComponentAccessor.getComponentOfType(LabelManager)
    def existingLabels = labelManager.getLabels(issue.id)*.label
    def labelsToSet = (existingLabels + label).toSet()
    def sendNotification = false
    def issueUpdateEventAndReindex = true
    labelManager.setLabels(executionUser, issue.id, labelsToSet, sendNotification, issueUpdateEventAndReindex)
}

static List<Issue> getIssuesInEpic(Issue epic) {
    ComponentAccessor.issueLinkManager.getOutwardLinks(epic.id).
            findAll { it.issueLinkType.name == "Epic-Story Link" }*.destinationObject
}

updateIssueLabels(currentUser, archLabel, issue)
getIssuesInEpic(issue).each { updateIssueLabels(currentUser, archLabel, it) }