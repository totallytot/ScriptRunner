package jira.post_functions.sub_task_creation_based_on

import com.atlassian.jira.component.ComponentAccessor

if (issue.issueType.subTask) return
def customFieldManager = ComponentAccessor.customFieldManager
def selectListVal = customFieldManager.getCustomFieldObjects(issue)
        .find { it.name == "Car" }?.getValue(issue) as String
if (!selectListVal) return

def subTaskIssueTypeId = ComponentAccessor.constantsManager.allIssueTypeObjects
        .find { it.name == "Sub-Task" }.id
def executionUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def issueService = ComponentAccessor.issueService
def newIssueInputParameters = issueService.newIssueInputParameters()
newIssueInputParameters.with {
    setSkipScreenCheck(true)
    setIssueTypeId(subTaskIssueTypeId as String)
    setProjectId(issue.projectId)
    setSummary("${selectListVal}: ${issue.summary}")
    setReporterId(executionUser.name)
    setPriorityId(issue.priority.id)
}
def createValidationResult = issueService.validateSubTaskCreate(executionUser, issue.id, newIssueInputParameters)
if (createValidationResult.valid) {
    def subTaskIssue = issueService.create(executionUser, createValidationResult).issue
    ComponentAccessor.subTaskManager.createSubTaskIssueLink(issue, subTaskIssue, executionUser)
} else createValidationResult.errorCollection