package jira.post_functions

import com.atlassian.jira.component.ComponentAccessor

def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def issueService = ComponentAccessor.issueService
def epicFields = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)

//set your values
def customSummary = "custom summary here "
def storyIssueTypeId = 10001
int storyAmount = 2

def createLinkedStory = {
    def storyInputParameters = issueService.newIssueInputParameters()
    storyInputParameters
            .setSummary(customSummary + epicFields.find {it.name == "Epic Name"}.getValue(issue))
            .setIssueTypeId(storyIssueTypeId as String)
            .setReporterId(currentUser.key)
            .setProjectId(issue.projectId)
            .addCustomFieldValue(epicFields.find {it.name == "Epic Link"}.id, issue.key)
            .setSkipScreenCheck(true)
    def createValidationResult = issueService.validateCreate(currentUser, storyInputParameters)
    if (createValidationResult.valid) issueService.create(currentUser, createValidationResult).issue
    else createValidationResult.errorCollection
}

if (issue.issueType.name == "Epic") storyAmount.times(createLinkedStory)