package jira.post_functions.set_field_based_on

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import java.text.SimpleDateFormat

def resolutionDate = issue.resolutionDate
if (!resolutionDate) return
def actualEndDate = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue).find {
    it.name == "Actual End Date" }
def executionUser = ComponentAccessor.userManager.getUserByName("tech_user")
def issueService = ComponentAccessor.issueService
def input = issueService.newIssueInputParameters()
input.addCustomFieldValue(actualEndDate.id,
        new SimpleDateFormat("dd/MMM/yy").format(resolutionDate)).setSkipScreenCheck(true)
IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(executionUser,
        issue.id, input)
log.info "Validation result: ${validationResult.valid}"
if (validationResult.valid) issueService.update(executionUser, validationResult).errorCollection
else log.info validationResult.errorCollection.toString()