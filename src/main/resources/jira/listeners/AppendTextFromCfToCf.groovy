package jira.listeners

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import java.text.SimpleDateFormat

def issue = event.issue
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser

def drbNotesValue = getCustomFieldValue("DRB Notes ", issue)
if (!drbNotesValue) return

def drbNotesAgrValue = getCustomFieldValue("DRB Notes (Aggregated)", issue)
def drbContributorsValue = getCustomFieldValue("DRB Contributors", issue) as List<ApplicationUser>

def customValue = """
Noted on: ${new SimpleDateFormat("dd/MMM/yyyy").format(new Date())} \n
By: ${user.displayName} \n
Contributors: ${drbContributorsValue*.displayName} \n
${drbNotesValue} \n
========================== \n
"""
def builder = new StringBuilder()
if (drbNotesAgrValue) builder.append(drbNotesAgrValue)
builder.append(customValue)

updateTextCf(user, issue, "DRB Notes (Aggregated)", builder.toString())
updateTextCf(user, issue, "DRB Notes ", null)
updateTextCf(user, issue, "DRB Contributors", null)

static def getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue).find { it.name == customFieldName }?.getValue(issue)
}

static updateTextCf(ApplicationUser executionUser, Issue issue, String fieldName, String text) {
    def textFieldObject = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue).find { it.name == fieldName }
    def issueService = ComponentAccessor.issueService
    def issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters.addCustomFieldValue(textFieldObject.id, text).setSkipScreenCheck(true)
    IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(executionUser,
            issue.id, issueInputParameters)
    if (validationResult.valid)
        issueService.update(executionUser, validationResult, EventDispatchOption.DO_NOT_DISPATCH, false)
    else validationResult.errorCollection
}