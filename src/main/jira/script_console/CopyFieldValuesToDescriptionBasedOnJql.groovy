package jira.script_console

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

final String JQL = 'issue in ("TEST-3123", "TEST-3114")'
final List<String> CUSTOM_FIELD_NAMES = ["Text 00", "Text 01", "Text 02"]

def executionUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def searchResult = getIssuesFromJql(executionUser, JQL)

if (searchResult.empty) return
def format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
def separator = """
___________________
The text below was added from archived custom fields on ${LocalDateTime.now().format(format)}
___________________
"""
searchResult.each { Issue issue ->
    def fieldValMapping = CUSTOM_FIELD_NAMES.collectEntries { [it, getCustomFieldValue(it, issue)] }
    def descVal = StringBuilder.newInstance()
    descVal << issue.description
    descVal << separator
    fieldValMapping.each { k, v ->
        descVal << k.toString().concat(": ").concat(v as String).concat("\\\\")
    }
    setDescription(executionUser, issue, descVal as String)
}

static List<Issue> getIssuesFromJql(ApplicationUser executionUser, String jql) {
    def searchService = ComponentAccessor.getComponentOfType(SearchService)
    def parseResult = searchService.parseQuery(executionUser, jql)
    if (parseResult.valid)
        searchService.search(executionUser, parseResult.query, PagerFilter.unlimitedFilter).results
    else []
}

static Object getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
            .find { it.name == customFieldName }?.getValue(issue)
}

static def setDescription(ApplicationUser executionUser, Issue issue, String value) {
    def issueService = ComponentAccessor.issueService
    def issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters.with {
        setSkipScreenCheck(true)
        setDescription(value)
    }
    IssueService.UpdateValidationResult validationResult = issueService
            .validateUpdate(executionUser, issue.id, issueInputParameters)
    if (validationResult.valid) issueService.update(executionUser, validationResult, EventDispatchOption.DO_NOT_DISPATCH, false)
    else validationResult.errorCollection
}