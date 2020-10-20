package jira.post_functions.sub_task_creation_based_on

import com.atlassian.jira.component.ComponentAccessor

// for testing in script console
def issue = ComponentAccessor.issueManager.getIssueObject("ITSDTEST-2")

def subTaskId = 10101
def issueService = ComponentAccessor.issueService
def subTaskManager = ComponentAccessor.subTaskManager
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def teamRoleValue = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue).find {
    it.id == "customfield_16920"
}.getValue(issue).toString()

def general = ["Jira", "Active Directory (Network Account)", "Office", "OpenAir", "LMS", "Concur"]
def finance = general.collect() << "NetSuite"
def engineering = general.collect() << "Wiki"

def createSubTask = { summary ->
    def subTaskInputParameters = issueService.newIssueInputParameters()
    subTaskInputParameters
            .setSummary(summary as String)
            .setIssueTypeId(subTaskId as String)
            .setReporterId(user.key)
            .setProjectId(issue.projectId)
            .setPriorityId(issue.priority.id)
            .setSkipScreenCheck(true)
    def createValidationResult = issueService.validateSubTaskCreate(user, issue.id, subTaskInputParameters)
    if (createValidationResult.valid) {
        def subTask = issueService.create(user, createValidationResult).issue
        subTaskManager.createSubTaskIssueLink(issue, subTask, user)
    }
    else createValidationResult.errorCollection
}

switch (teamRoleValue) {
    case "General":
        general.each{ createSubTask(it)}
        break
    case "Finance":
        finance.each{createSubTask(it)}
        break
    case "Engineering":
        engineering.each{createSubTask(it)}
        break
    default:
        break
}