package jira.listeners.set_field_based_on

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import java.text.SimpleDateFormat

def issue = event.issue
if (issue.issueType.name != "Task") return
log.info "Working with ${issue}"
def actualStartDate = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue).find {
    it.name == "Actual Start Date" }
def actualStartDateValue = actualStartDate?.getValue(issue)
if (!actualStartDateValue && issue.status.statusCategory.name == "In Progress") {
    def executionUser = ComponentAccessor.userManager.getUserByName("tech_user")
    def issueService = ComponentAccessor.issueService
    def issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters.addCustomFieldValue(actualStartDate.id,
            new SimpleDateFormat("dd/MMM/YY").format(new Date())).setSkipScreenCheck(true)
    IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(executionUser,
            issue.id, issueInputParameters)
    log.info "Validation result: ${validationResult.valid}"
    if (validationResult.valid) issueService.update(executionUser, validationResult).errorCollection
    else log.info validationResult.errorCollection.toString()
} else log.info "Condition status: " + (!actualStartDateValue && issue.status.statusCategory.name == "In Progress")