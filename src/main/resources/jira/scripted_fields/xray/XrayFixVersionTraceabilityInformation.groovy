package jira.scripted_fields.xray

import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter

if (!issue.fixVersions) return
def issueLinkManager = ComponentAccessor.issueLinkManager
def executionUser = ComponentAccessor.userManager.getUserByName("admin")
def stringBuilder = new StringBuilder()

issue.fixVersions.each { fixVersion ->
    stringBuilder.append("<b>${fixVersion.name}:</b><br>")
    def frJql = """issuetype = "Functional Requirement" and fixVersion = ${fixVersion.name}"""
    def frIssues = getIssuesFromJql(executionUser, frJql)
    if (!frIssues) return
    frIssues.each { frIssue ->
        def linkedTests = issueLinkManager.getInwardLinks(frIssue.id)?.findResults { inwardLink ->
            inwardLink.issueLinkType.name == "Requirement" && inwardLink.sourceObject.issueType.name == "Test" &&
                    inwardLink.sourceObject.fixVersions.any { it.name == fixVersion.name }
                    ? inwardLink.sourceObject : null
        }
        if (!linkedTests.isEmpty()) {
            stringBuilder.append("Functional Requirement ${frIssue.key} is tested by: ")
            stringBuilder.append("${linkedTests}<br>")
        }
    }
    def teJql = """issuetype = "Test Execution" and fixVersion = ${fixVersion.name}"""
    def teIssues = getIssuesFromJql(executionUser, teJql)
    def mapping = new HashMap<String, List<Issue>>()
    if (!teIssues.isEmpty()) {
        teIssues.each { testExecutionIssue ->
            def testIssueKeys = getCustomFieldValue("Tests association with a Test Execution",
                    testExecutionIssue) as List
            testIssueKeys.each { testIssueKey ->
                if (!mapping.get(testIssueKey as String)) mapping.put(testIssueKey as String, [testExecutionIssue as Issue])
                else mapping.get(testIssueKey as String) << testExecutionIssue
            }
        }
        mapping.each { k, v ->
            stringBuilder.append(k).append(" test executions: ").append("${v.key}<br>")
            v.groupBy { it.created }
            def latestTe = v.first()
            stringBuilder.append("Latest is ${latestTe.key}")
            def latestTeStatus = getCustomFieldValue("Test Execution Status", latestTe)
            stringBuilder.append(" with status: ${latestTeStatus}<br>")
        }
    }
}
return stringBuilder.toString()

static List<Issue> getIssuesFromJql(ApplicationUser executionUser, String jql) {
    def searchService = ComponentAccessor.getComponentOfType(SearchService)
    def parseResult = searchService.parseQuery(executionUser, jql)
    if (parseResult.valid)
        searchService.search(executionUser, parseResult.query, PagerFilter.unlimitedFilter).results
    else null
}

static Object getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
            .find { it.name == customFieldName }?.getValue(issue)
}