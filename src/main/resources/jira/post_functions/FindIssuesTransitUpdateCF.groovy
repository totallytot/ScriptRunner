package jira.post_functions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField

def issueService = ComponentAccessor.issueService
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def issueManager = ComponentAccessor.issueManager
def smartClonedFromField = ComponentAccessor.customFieldManager.getCustomFieldObject(10400)
def inProductTestField = ComponentAccessor.customFieldManager.getCustomFieldObject(10500)
def transitionId = 171
def maxAllowedLoop = 60
def updateStringField = { CustomField field, MutableIssue localIssue, String value ->
    def inputParameters = issueService.newIssueInputParameters()
    inputParameters.addCustomFieldValue(field.id, value).setSkipScreenCheck(true)
    def validationResult = issueService.validateUpdate(user, localIssue.id, inputParameters)
    if (validationResult.isValid()) {
        issueService.update(user, validationResult)
    } else {
        log.warn("update errors " + validationResult.errorCollection)
    }
}

def transitIssueToReplace = { MutableIssue localIssue ->
    def transitionValidationResult = issueService.validateTransition(user, localIssue.id, transitionId,
            issueService.newIssueInputParameters())
    if (transitionValidationResult.isValid()) {
        issueService.transition(user, transitionValidationResult)
    } else {
        log.warn("transition errors " + transitionValidationResult.errorCollection)
    }
}

updateStringField(inProductTestField, issue, issue.key)
def issueForTransition = issueManager?.getIssueObject(smartClonedFromField?.getValueFromIssue(issue) as String)
def counter = 0

if (issueForTransition) {
    counter++
    transitIssueToReplace(issueForTransition)
    updateStringField(inProductTestField, issueForTransition, issue.key)
    def isRunning = true
    while (isRunning) {
        issueForTransition = issueManager?.getIssueObject(issueForTransition?.getCustomFieldValue(smartClonedFromField) as String)
        if (issueForTransition == null || issueForTransition.key == issue.key || counter > maxAllowedLoop) isRunning = false
        if (issueForTransition) {
            transitIssueToReplace(issueForTransition)
            updateStringField(inProductTestField, issueForTransition, issue.key)
        }
    }
}