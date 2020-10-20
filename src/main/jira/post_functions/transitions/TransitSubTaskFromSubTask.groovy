package jira.post_functions.transitions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue

def parentIssue = issue.parentObject
def issueService = ComponentAccessor.issueService
def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
parentIssue.subTaskObjects.findAll { it.status.name == "Released" }?.each { issue ->
    issue = issue as Issue
    def issueInputParameters = issueService.newIssueInputParameters()
    IssueService.TransitionValidationResult transitionValidationResult =
            issueService.validateTransition(currentUser, issue.id, 321, issueInputParameters)
    if (transitionValidationResult.isValid()) {
        issueService.transition(currentUser, transitionValidationResult)
    }
}