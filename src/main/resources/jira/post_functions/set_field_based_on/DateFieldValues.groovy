package jira.post_functions.set_field_based_on

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import java.text.SimpleDateFormat

log.info "Working with ${issue.key}"
def customFields = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)

// values for checking
def plannedStartDateVal = customFields.find { it.name == "Planned Start date"}?.getValue(issue)
def plannedEndDateVal = customFields.find { it.name == "Planned End Date"}?.getValue(issue)
def plannedEffortVal = customFields.find { it.name == "Planned Effort"}?.getValue(issue)

// fields to update
def baselineStartDate = customFields.find { it.name == "Baseline start date"}
def baselineEndDate = customFields.find { it.name == "Baseline end date"}
def baselineEffort = customFields.find { it.name == "Baseline Effort"}

def executionUser = ComponentAccessor.userManager.getUserByName("tech_user")
def issueService = ComponentAccessor.issueService
def input = issueService.newIssueInputParameters()
input.with {
    setSkipScreenCheck(true)
    if (plannedStartDateVal) addCustomFieldValue(baselineStartDate.id,
            new SimpleDateFormat("dd/MMM/yy").format(plannedStartDateVal))
    if (plannedStartDateVal) addCustomFieldValue(baselineEndDate.id,
            new SimpleDateFormat("dd/MMM/yy").format(plannedEndDateVal))
    if (plannedEffortVal) addCustomFieldValue(baselineEffort.id, plannedEffortVal as String)
}
IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(executionUser,
        issue.id, input)
if (validationResult.valid) issueService.update(executionUser, validationResult).errorCollection
else log.info validationResult.errorCollection as String