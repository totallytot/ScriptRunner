package jira.script_console

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import groovy.transform.Field

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Field final String JQL = "issue in (CFSU-785)"
@Field final List<String> CUSTOM_FIELD_NAMES = ["Database", "Reproducible"]
@Field final List<String> SYSTEM_FIELD_NAMES = ["Environment"]

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
"""
searchResult.each { Issue issue ->
    def customFieldValMapping = CUSTOM_FIELD_NAMES.collectEntries { [it, getCustomFieldValue(it, issue)] }
    def hasCustomFieldValue = customFieldValMapping.values().any { it != null }
    def systemFieldValMapping = SYSTEM_FIELD_NAMES.collectEntries { [it, getSystemFieldValue(it, issue)] }
    def hasSystemFieldValue = systemFieldValMapping.values().any { it != null }

    if (hasCustomFieldValue || hasSystemFieldValue) {
        def descVal = StringBuilder.newInstance()
        if (issue.description) descVal << issue.description
        descVal << mainSeparator
        def commonFieldValMapping = customFieldValMapping + systemFieldValMapping
        commonFieldValMapping.each { k, v ->
            if (v) {
                descVal << k.toString().concat(": ").concat(v as String)
                descVal << fieldSeparator
            }
        }
        def result = setDescription(executionUser, issue, descVal as String)
        log.warn "Working with ${issue.key}"
        log.warn "Update result: ${result}"
    }
}

static List<Issue> getIssuesFromJql(ApplicationUser executionUser, String jql) {
    def searchService = ComponentAccessor.getComponentOfType(SearchService)
    def parseResult = searchService.parseQuery(executionUser, jql)
    if (parseResult.valid)
        searchService.search(executionUser, parseResult.query, PagerFilter.unlimitedFilter).results
    else []
}

static Object getSystemFieldValue(String systemFieldName, Issue issue) {
    def value = null
    switch (systemFieldName.toLowerCase()) {
        case "environment":
            value = issue.environment
            break
    }
    return value
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