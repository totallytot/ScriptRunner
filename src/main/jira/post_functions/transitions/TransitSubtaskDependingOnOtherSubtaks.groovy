package jira.post_functions.transitions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue

def issue = ComponentAccessor.issueManager.getIssueObject("PFT-65") //for testing in script console only

def readyTransitionId = 31
def conditionSubTaskSummaries = ["Splunk Trending Queries and Supporting Queries", "Create Rule in Dev splunk ES",
                                 "Content Peer Review", "mitre mapping", "Create Rule in Prod for Test",
                                 "soc qa"].collect { it.toLowerCase() }
def conditionStatus = "Done"
def transitionSubTaskSummaries = ["Soc approval"].collect { it.toLowerCase() }
def assigneeUsername = "user"
def scriptExecutor = "user"

def conditionSubTasks = issue.parentObject.subTaskObjects.findAll {
    it.summary.toLowerCase() in conditionSubTaskSummaries
}
def transitionSubTasks = issue.parentObject.subTaskObjects.findAll {
    it.summary.toLowerCase() in transitionSubTaskSummaries
}
def transitionCondition = conditionSubTasks.every {
    it.summary.toLowerCase() in conditionSubTaskSummaries &&
            it.status.name == conditionStatus && conditionSubTaskSummaries.size() == conditionSubTasks.size()
}

def transitionIssue = { Issue localIssue ->
    ComponentAccessor.jiraAuthenticationContext.setLoggedInUser(ComponentAccessor.userManager.getUserByKey(scriptExecutor))
    def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
    def issueService = ComponentAccessor.issueService
    def issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters.setSkipScreenCheck(true)
    issueInputParameters.setAssigneeId(assigneeUsername)
    IssueService.TransitionValidationResult transitionValidationResult =
            issueService.validateTransition(currentUser, localIssue.id, readyTransitionId, issueInputParameters)
    if (transitionValidationResult.valid) issueService.transition(currentUser, transitionValidationResult)
    else transitionValidationResult.errorCollection
}
if (transitionCondition && transitionSubTasks) transitionSubTasks.each { transitionIssue(it) }