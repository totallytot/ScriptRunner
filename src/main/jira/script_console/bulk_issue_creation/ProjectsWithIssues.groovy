package jira.script_console.bulk_issue_creation

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.project.ProjectService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.project.ProjectCreationData.Builder

final int amountOfProjects = 5
final String prefixKey = "TEST"
final String projectType = "business"
final int amountOfProjectIssues = 2000
final String issueType = "Story"
final String priority = "Medium"

def executionUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def projectService = ComponentAccessor.getComponent(ProjectService)
def issueService = ComponentAccessor.issueService
def issueTypeId = ComponentAccessor.constantsManager.allIssueTypeObjects.find { it.name == issueType }.id
def priorityId = ComponentAccessor.constantsManager.priorities.find { it.name == priority }.id

amountOfProjects.times { int counter ->
    def builder = new Builder()
    builder.with {
        withKey(prefixKey + counter as String)
        withName(prefixKey + counter as String)
        withLead(executionUser)
        withDescription(prefixKey + counter as String)
        withType(projectType)
    }
    def projectData = builder.build()
    def projectValidationResult = projectService.validateCreateProject(executionUser, projectData)
    if (projectValidationResult.valid) {
        def project = projectService.createProject(projectValidationResult)
        amountOfProjectIssues.times {
            def issueInputParameters = issueService.newIssueInputParameters()
            issueInputParameters.with {
                setProjectId(project.id)
                setSummary("Test")
                setDescription("Test")
                setIssueTypeId(issueTypeId)
                setReporterId(executionUser.username)
                setPriorityId(priorityId)
            }
            IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(executionUser, issueInputParameters)
            if (createValidationResult.valid) issueService.create(executionUser, createValidationResult)
            else createValidationResult.errorCollection.errorMessages
        }
    } else projectValidationResult.errorCollection.errorMessages
}