package jira.post_functions.transitions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue

//for testing in script console
def issue = ComponentAccessor.issueManager.getIssueObject("PFT-65")
def epicTransitionId = 91
def summaryCheck = "Prioritization Assessment"
def epic = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue.parentObject).find {
    it.name == "Epic Link"
}?.getValue(issue.parentObject) as Issue

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

def condition = epic && issue.parentObject.issueType.name == "Story" && epic.status.name != "Priority Review" &&
        issue.parentObject.summary.toLowerCase().contains(summaryCheck.toLowerCase()) &&
        issue.parentObject.subTaskObjects.every { it.status.name == "Done" }
if (condition) transitionIssue(epic)