package jira.post_functions.transitions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor

def issuesForTransition = ComponentAccessor.issueLinkManager.getInwardLinks(issue.id).findAll {
    it.issueLinkType.name == "Tests" && it.sourceObject.status.name == "Done"
}.collect { it.sourceObject }
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def issueService = ComponentAccessor.issueService
def issueInputParameters = issueService.newIssueInputParameters()
if (issuesForTransition) {
    issuesForTransition.each {
        IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(user,
                it.id, 161, issueInputParameters)
        if (transitionValidationResult.isValid()) {
            issueService.transition(user, transitionValidationResult)
        }
    }
}