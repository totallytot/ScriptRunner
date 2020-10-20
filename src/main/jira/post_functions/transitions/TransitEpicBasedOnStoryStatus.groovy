package jira.post_functions.transitions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue

//for testing in script console
def issue = ComponentAccessor.issueManager.getIssueObject("PFT-65")
def summaryCheck = "Prioritization Assessment"
def epicTransitionId = 101
def epic = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue).find {
    it.name == "Epic Link"
}?.getValue(issue) as Issue

def transitionIssue = { Issue localIssue ->
    def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
    def issueService = ComponentAccessor.issueService
    def issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters.setSkipScreenCheck(true)
    IssueService.TransitionValidationResult transitionValidationResult =
            issueService.validateTransition(currentUser, localIssue.id, epicTransitionId, issueInputParameters)
    if (transitionValidationResult.valid) issueService.transition(currentUser, transitionValidationResult)
    else transitionValidationResult.errorCollection
}

def condition = epic && epic.status.name != "Ready" && issue.issueType.name == "Story" &&
        issue.summary.toLowerCase().contains(summaryCheck.toLowerCase())
if (condition) transitionIssue(epic)
