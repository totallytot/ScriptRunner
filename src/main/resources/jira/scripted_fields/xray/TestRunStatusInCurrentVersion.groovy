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
def executionUser = ComponentAccessor.userManager.getUserByName("admin")
def jql = "issuetype = 'Test Execution' and issue in testTestExecutions('${issue.key}') and fixVersion = '${currentVersionFieldVal.first().toString()}'"
def searchResult = getIssuesFromJql(executionUser, jql)
searchResult.groupBy { it.created }
def latestTestExecution = searchResult.first()

// REST
def authString = "admin:TryHarder".bytes.encodeBase64().toString()
def baseurl = ComponentAccessor.applicationProperties.getString("jira.baseurl")
def http = new HTTPBuilder("${baseurl}/rest/raven/1.0/api/test/${issue.key}/testruns")
def testRunData = null
http.request(Method.GET, ContentType.JSON) { req ->
    headers."Authorization" = "Basic ${authString}"
    response.success = { resp, reader ->
        testRunData = reader.find {
            it.testExecKey == latestTestExecution.key
        }
    }
}

// output
if (!testRunData) return
def stringBuilder = new StringBuilder()
stringBuilder.with {
    append("Latest Test Execution is ${testRunData.testExecKey}<br>")
    append("Id: ${testRunData.id}<br>")
    append("Status: ${testRunData.status}<br>")
    append("Finished On: ${testRunData.finishedOn}")
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