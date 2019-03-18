package jira.postfunctions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor

def parentIssue = issue.parentObject
def subTaskIssueToTransit = ComponentAccessor.issueLinkManager.getOutwardLinks(parentIssue.id)
        .find{it.destinationObject.status.name == "Released"}?.destinationObject

if (subTaskIssueToTransit) {
    def issueService = ComponentAccessor.issueService
    def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
    def issueInputParameters = issueService.newIssueInputParameters()
    IssueService.TransitionValidationResult transitionValidationResult =
            issueService.validateTransition(currentUser, subTaskIssueToTransit.id, 321, issueInputParameters)
    if (transitionValidationResult.isValid()) {
        issueService.transition(currentUser, transitionValidationResult)
    }
}