package jira.script_console

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter

String user = "tech_user"
String jqlQuery = "issuekey = VM-1"
String filePath = "/opt/jira_data/scriptrunner/mapping.txt"

def issueService = ComponentAccessor.getIssueService()
def searchService = ComponentAccessor.getComponentOfType(SearchService.class)
def userManager = ComponentAccessor.getUserManager()

ApplicationUser applicationUser = userManager.getUserByKey(user)

//get data from file
def mapping = new HashMap<String, String>()
def sourceFile = new File(filePath)
sourceFile.eachLine {line ->
    String[] formatedArr = line.replaceAll("\"", "").split(",")
    mapping.put(formatedArr[0], formatedArr[1])
}

//get issues from JQL query
final SearchService.ParseResult parseResult = searchService.parseQuery(applicationUser, jqlQuery)
List<Issue> issues = new ArrayList<>()
if (parseResult.isValid()) {
    final SearchResults results = searchService.search(applicationUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
    issues = results.getIssues()
}

//issues.each {issue ->
def issue = issues.get(0)
IssueInputParameters issueInputParameters = issueService.newIssueInputParameters()
def assignee = (ApplicationUser) issue.getAssignee()
def reporter = (ApplicationUser) issue.getReporter()

if (assignee != null && mapping.containsKey(assignee.getKey())) issueInputParameters.setAssigneeId(mapping.get(assignee.getKey()))
if (reporter != null && mapping.containsKey(reporter.getKey())) issueInputParameters.setReporterId(mapping.get(reporter.getKey()))

IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(applicationUser, issue.getId(), issueInputParameters)
if (validationResult.isValid()) {
    issueService.update(applicationUser, validationResult)
}


