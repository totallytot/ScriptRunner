package jira.postfunctions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor

def parentIssue = issue.parentObject
def subTaskIssueToTransit = parentIssue.subTaskObjects.findAll{it.status.name == "Released"}

if (subTaskIssueToTransit) {
    def issueService = ComponentAccessor.issueService
    def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
    subTaskIssueToTransit.each{
        def issueInputParameters = issueService.newIssueInputParameters()
        IssueService.TransitionValidationResult transitionValidationResult =
                issueService.validateTransition(currentUser, it.id, 321, issueInputParameters)
        if (transitionValidationResult.isValid()) {
            issueService.transition(currentUser, transitionValidationResult)
        }
    }
}