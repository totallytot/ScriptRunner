package jira.postfunctions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue

/**
 *  subtasks 1-5 (all standard subtasks) when done(can be done in any order), automatically transition subtask 7 to its next status, “ready”.
 *  I assume this is a post function that will be applied to subtasks 1-5 (just use placeholder names I will replace,
 *  they are all standard subtasks but have unique summaries) workflow.
 *  A check will happen as each one is finished, but will be blocked until content approval condition is satisfiedRobert
 *  Another problem, there are STANDARD subtasks AFTER 6 and 7, and we don’t want those statuses blocking subtask 6 and 7
 *  all subtasks have unique summaries. I think the script needs to call out the summaries, not the issue types
 *  Yep, we can use summaries in the script instead of sub-task order.
 */

def issue = ComponentAccessor.issueManager.getIssueObject("PFT-65") //for testing in script console only
def readyTransitionId = 41
def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def subTaskSummaries = ["summary1", "summary2", "summary3", "summary4", "summary5", "summary6", "summary7"].collect {
    it.toLowerCase()
}
def conditionSubTasks = issue.parentObject.subTaskObjects.findAll {
    it.summary.toLowerCase() in subTaskSummaries.take(5)
} as ArrayList<Issue>
def transitionSubTasks = issue.parentObject.subTaskObjects.findAll {
    it.summary.toLowerCase() in subTaskSummaries.subList(5, 7)
}
def transitionCondition = conditionSubTasks.every { it.status.name == "Finished" }
def transitionIssue = { Issue localIssue ->
    def issueService = ComponentAccessor.issueService
    def issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters.setSkipScreenCheck(true)
    IssueService.TransitionValidationResult transitionValidationResult =
            issueService.validateTransition(currentUser, localIssue.id, readyTransitionId, issueInputParameters)
    if (transitionValidationResult.valid) issueService.transition(currentUser, transitionValidationResult)
    else transitionValidationResult.errorCollection
}
if (transitionCondition && transitionSubTasks) transitionSubTasks.each { transitionIssue(it) }