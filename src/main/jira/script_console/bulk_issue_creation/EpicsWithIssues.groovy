package jira.script_console.bulk_issue_creation

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder

int amountOfEpics = 100
int amountOfIssuesInEpic = 15
def issueTypeID = "10000" // Epic
def projectId = 10306 // Test
def priorityId = "4"

def issueService = ComponentAccessor.issueService
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser

amountOfEpics.times {
    def epicIssueInputParameters = issueService.newIssueInputParameters()
    epicIssueInputParameters
            .setProjectId(projectId)
            .setSummary("Epic Test")
            .setDescription("Test desc")
            .setIssueTypeId(issueTypeID)
            .setPriorityId(priorityId)
            .setReporterId(user.username)
            .addCustomFieldValue(10103, "WithIssues")

    IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(user, epicIssueInputParameters)
    log.error("issue input parameters: " + epicIssueInputParameters.getActionParameters())
    def epicKey
    if (createValidationResult.isValid()) {
        IssueService.IssueResult createResult = issueService.create(user, createValidationResult)
        epicKey = createResult.issue.key
        if (!createResult.isValid()) log.error("Error while creating the issue.")
    } else createValidationResult.errorCollection.errorMessages

    def createIssueInEpic = { String epicIssueKey ->
        def issueInputParameters = issueService.newIssueInputParameters()
        issueInputParameters
                .setProjectId(projectId)
                .setSummary("Test")
                .setDescription("Test desc")
                .setIssueTypeId("10001")
                .setPriorityId(priorityId)
                .setReporterId(user.username)

        IssueService.CreateValidationResult validationResult = issueService.validateCreate(user, issueInputParameters)
        def issueInEpic
        if (validationResult.isValid()) {
            IssueService.IssueResult createResult = issueService.create(user, validationResult)
            issueInEpic = createResult.issue
        } else validationResult.errorCollection.errorMessages

        def customFieldManager = ComponentAccessor.getCustomFieldManager()
        def issueManager = ComponentAccessor.getIssueManager()
        def epicIssue = issueManager.getIssueObject(epicIssueKey) // Epic issue key here
        def epicLink = customFieldManager.getCustomFieldObjectByName("Epic Link")
        epicLink.updateValue(null, issueInEpic, new ModifiedValue(issueInEpic.getCustomFieldValue(epicLink), epicIssue),
                new DefaultIssueChangeHolder())
    }

    amountOfIssuesInEpic.times { createIssueInEpic(epicKey) }
}