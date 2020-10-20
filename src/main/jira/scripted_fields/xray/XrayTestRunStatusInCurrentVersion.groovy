package jira.scripted_fields.xray

import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

if (issue.issueType.name != "Test") return
def currentVersionFieldVal = getCustomFieldValue("Current version", issue as Issue) as List
if (!currentVersionFieldVal) return "Unknown (Current Version is empty)"

// find the most recent test run of this test
def executionUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def jql = """issuetype = 'Test Execution' and issue in testTestExecutions('${issue.key}') 
and fixVersion = '${currentVersionFieldVal.first().toString()}' order by created"""
def searchResult = getIssuesFromJql(executionUser, jql)
if (searchResult.empty) return "JQL search result is empty"
def latestTestExecution = searchResult.first()

// REST
def authString = "admin:TryHarder".bytes.encodeBase64().toString()
def baseurl = ComponentAccessor.applicationProperties.getString("jira.baseurl")
boolean isErrorResponse = false
def http = new HTTPBuilder("${baseurl}/rest/raven/1.0/api/test/${issue.key}/testruns")
def testRunData = http.request(Method.GET, ContentType.JSON) { req ->
    headers."Authorization" = "Basic ${authString}"
    response.success = { resp, reader ->
        assert resp.status == 200
        def testExecutionData = reader.find { it.testExecKey == latestTestExecution.key }
        if (!testExecutionData) {
            isErrorResponse = true
            return "Response status is ${resp.status}, but there is no match on Test Executions."
        } else return testExecutionData
    }
    response.failure = { resp ->
        isErrorResponse = true
        return resp.statusLine
    }
}

// output
if (isErrorResponse) return testRunData
def stringBuilder = new StringBuilder()
stringBuilder.with {
    append("Latest Test Execution is ${testRunData.testExecKey}<br>")
    append("Id: ${testRunData.id}<br>")
    append("Status: ${testRunData.status}<br>")
    append("Finished on: ${testRunData.finishedOn}")
}
return stringBuilder.toString()

static Object getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
            .find { it.name == customFieldName }?.getValue(issue)
}

static List<Issue> getIssuesFromJql(ApplicationUser executionUser, String jql) {
    def searchService = ComponentAccessor.getComponentOfType(SearchService)
    def parseResult = searchService.parseQuery(executionUser, jql)
    if (parseResult.valid)
        searchService.search(executionUser, parseResult.query, PagerFilter.unlimitedFilter).results
    else null
}