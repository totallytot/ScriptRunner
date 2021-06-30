package jira.post_functions.create_issue

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import groovy.transform.Field
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder

@Field final String TEAM_CITY_CUSTOM_BUILD_URL = "https://teamcity.example.com/app/rest/buildQueue"
@Field final String TEAM_CITY_BASIC_AUTH = "Basic "
@Field final String JIRA_EXECUTION_USERNAME = "service_user"
@Field final String PROJECT_KEY = "Test"
@Field final String ISSUE_TYPE_NAME = "Report"
@Field final String LINK_TYPE_NAME = "Relate"

def issueService = ComponentAccessor.issueService
def issueLinkManager = ComponentAccessor.issueLinkManager
def projectManager = ComponentAccessor.projectManager
def constantsManager = ComponentAccessor.constantsManager
def issueLinkTypeManager = ComponentAccessor.getComponent(IssueLinkTypeManager)

def issueInputParameters = issueService.newIssueInputParameters()
def linkTypes = issueLinkTypeManager.getIssueLinkTypes(false)
def applicationUser = ComponentAccessor.userManager.getUserByName(JIRA_EXECUTION_USERNAME)
if (!applicationUser) return

def versionVal = getCustomFieldValue("VersionDi", issue)
def versionName = issue.summary
issueInputParameters.with {
    setProjectId(projectManager.getProjectByCurrentKey(PROJECT_KEY).id)
    setIssueTypeId(constantsManager.allIssueTypeObjects.find { it.name == ISSUE_TYPE_NAME }.id)
    setReporterId(applicationUser.username)
    setSummary("${versionName} ${versionVal}. Паспорт.")
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

def json = """
{
    "buildType": {         
        "id": "Test"     
    },
    "comment": {
        "text": " REST API"     
    },
    "properties": {
        "property": [
            {
                "name": "Test",                 
                "value": "${versionName}"             
            }
        ]
    }
}
"""
def httpClient = HttpClientBuilder.create().build()
def httpPost = new HttpPost(TEAM_CITY_CUSTOM_BUILD_URL)
httpPost.with {
    setHeader("Authorization", TEAM_CITY_BASIC_AUTH)
    setEntity(new StringEntity(json, ContentType.APPLICATION_JSON))
}
def response = httpClient.execute(httpPost)
log.warn(response.statusLine as String)

static Object getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
            .find { it.name == customFieldName }?.getValue(issue)
}