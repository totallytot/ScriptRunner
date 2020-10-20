package jira.post_functions.transitions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue

def issue = ComponentAccessor.issueManager.getIssueObject("PFT-65") //for testing in script console only

def storyTransitionId = 21
def summaryCheckA = "-Data Ingestion"
def summaryCheckB = "-Content Story"
def parentIssue = issue?.parentObject

def transitionIssue = { Issue localIssue ->
    def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
    def issueService = ComponentAccessor.issueService
    def issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters.setSkipScreenCheck(true)
    IssueService.TransitionValidationResult transitionValidationResult =
            issueService.validateTransition(currentUser, localIssue.id, storyTransitionId, issueInputParameters)
    if (transitionValidationResult.valid) issueService.transition(currentUser, transitionValidationResult)
    else transitionValidationResult.errorCollection
}

if (parentIssue && parentIssue.issueType.name == "Story" && parentIssue.status.name != "In Progress" &&
        (parentIssue.summary.toLowerCase().contains(summaryCheckA.toLowerCase()) ||
                parentIssue.summary.toLowerCase().contains(summaryCheckB.toLowerCase()))) {
    transitionIssue(parentIssue)
}