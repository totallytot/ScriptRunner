package jira.post_functions.create_issue

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import groovy.transform.Field

@Field final String EXECUTION_USERNAME = "service_users"
@Field final String PROJECT_KEY = "TEST"
@Field final String ISSUE_TYPE_NAME = "Task"
@Field final String LINK_TYPE_NAME = "Relate"

def issueService = ComponentAccessor.issueService
def projectManager = ComponentAccessor.projectManager
def constantsManager = ComponentAccessor.constantsManager
def issueLinkManager = ComponentAccessor.issueLinkManager
def issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager)
def linkTypes = issueLinkTypeManager.getIssueLinkTypes(false)

if (issue.issueType.name != "Improvement") return
def conditionFieldVal = getCustomFieldValue("BSS", issue)
if (!conditionFieldVal || !conditionFieldVal.toString().contains("SYNC_ID")) return

def applicationUser = ComponentAccessor.userManager.getUserByName(EXECUTION_USERNAME)
if (!applicationUser) return

def issueInputParameters = issueService.newIssueInputParameters()
issueInputParameters.with {
    setProjectId(projectManager.getProjectByCurrentKey(PROJECT_KEY).id)
    setIssueTypeId(constantsManager.allIssueTypeObjects.find { it.name == ISSUE_TYPE_NAME }.id)
    setReporterId(applicationUser.username)
    setSummary("${issue.key} ${issue.summary}")
    setSkipScreenCheck(true)
}

def createValidationResult = issueService.validateCreate(applicationUser, issueInputParameters)
if (!createValidationResult.valid) return createValidationResult.errorCollection
def result = issueService.create(applicationUser, createValidationResult)
if (!result.valid) return result.errorCollection

issueLinkManager.createIssueLink(
        issue.id,
        result.issue.id,
        linkTypes.find { it.name == LINK_TYPE_NAME }.id,
        1,
        applicationUser
)

static Object getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
            .find { it.name == customFieldName }?.getValue(issue)
}