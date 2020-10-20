package jira.post_functions

import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter

String sourceSummary = issue.summary

String user = "admin"
String jqlQuery = "project = PELATEST and summary ~ '" + sourceSummary + "' order by key desc"

def searchService = ComponentAccessor.getComponentOfType(SearchService.class)
def userManager = ComponentAccessor.getUserManager()
ApplicationUser applicationUser = userManager.getUserByKey(user)

//get issues from JQL query
final SearchService.ParseResult parseResult = searchService.parseQuery(applicationUser, jqlQuery)
List<Issue> issues = new ArrayList<>()
if (parseResult.isValid()) {
    final SearchResults results = searchService.search(applicationUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
    issues = results.getIssues()
}

List<Issue> destinationIssues = new ArrayList<>()

if (issues.size() > 1) {
    issues.remove(0)
    destinationIssues = issues.findAll{it.summary==sourceSummary}
}

if (destinationIssues.size() > 0) {
    def isssueLinkManager = ComponentAccessor.issueLinkManager
    destinationIssues.each {
        isssueLinkManager.createIssueLink(issue.getId(), it.getId(), 10003L, 1L, applicationUser) //10001 - id of clone link
    }
}