package jira.script_console.bulk_issue_creation

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor

int amountOfEpics = 1500
def issueTypeID = "10000" // Epic
def projectId = 10306 // Test
def priorityId = "4"

def issueService = ComponentAccessor.issueService
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser

amountOfEpics.times {
    def issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters
            .setProjectId(projectId)
            .setSummary("Epic Test")
            .setDescription("test desc")
            .setIssueTypeId(issueTypeID)
            .setPriorityId(priorityId)
            .setReporterId(user.username)
            .addCustomFieldValue(10103, "test")

    IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(user, issueInputParameters)
    log.error("issue input parameters: " + issueInputParameters.getActionParameters())
    if (createValidationResult.isValid())
    {
        IssueService.IssueResult createResult = issueService.create(user, createValidationResult)
        if (!createResult.isValid()) log.error("Error while creating the issue.")
    }
    else createValidationResult.errorCollection.errorMessages
}