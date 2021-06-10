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

final String JQL = "issue in (CFSU-769)"
final List<String> CUSTOM_FIELD_NAMES = ["Database", "Reproducible"]

def executionUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def searchResult = getIssuesFromJql(executionUser, JQL)

if (searchResult.empty) return
def format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
def mainSeparator = """
___________________
The text below was added from archived custom fields on ${LocalDateTime.now().format(format)}
___________________
"""
def fieldSeparator = """
Currently - ___________________
Reason For Chosen Priority: Technical documents are client facing.
Steps To Reproduce : Run a month end close in the system.
"""
searchResult.each { Issue issue ->
    def fieldValMapping = CUSTOM_FIELD_NAMES.collectEntries { [it, getCustomFieldValue(it, issue)] }
    def descVal = StringBuilder.newInstance()
    if (issue.description) descVal << issue.description
    def hasValue = fieldValMapping.values().any { it != null}
    if (hasValue) {
        descVal << mainSeparator
        fieldValMapping.each { k, v ->
            if (v) {
                descVal << k.toString().concat(": ").concat(v as String)
                descVal << fieldSeparator
            }
        }
        def result = setDescription(executionUser, issue, descVal as String)
        log.warn """Working with ${issue.key}"""
        log.warn """Update result: ${result}"""
    }
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