package jira.script_console

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import groovy.transform.Field
import org.apache.log4j.Level
import org.apache.log4j.Logger

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Field final String JQL = "issue in (PFTA-110)"
@Field final List<String> CUSTOM_FIELD_NAMES = ["Car", "External Issue"]
@Field final List<String> SYSTEM_FIELD_NAMES = ["Environment"]

def logger = Logger.getLogger("FieldAppender")
logger.setLevel(Level.INFO)
logger.info "SCRIPT START"

def setDescription = { ApplicationUser executionUser, Issue issue, String value ->
    logger.info "Starting description update..."
    def issueService = ComponentAccessor.issueService
    def issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters.with {
        setSkipScreenCheck(true)
        setDescription(value)
    }
    IssueService.UpdateValidationResult validationResult = issueService
            .validateUpdate(executionUser, issue.id, issueInputParameters)
    if (!validationResult.valid) {
        logger.error "Issue update validation result is not valid: ${validationResult.errorCollection}"
        return
    }
    def updateResult = issueService.update(executionUser, validationResult, EventDispatchOption.DO_NOT_DISPATCH, false)
    if (!updateResult.valid) {
        logger.error "Issue update result is not valid: ${updateResult.errorCollection}"
        return
    }
    logger.info "Description has been updated"
}

def executionUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def searchResult = getIssuesFromJql(executionUser, JQL)
if (searchResult.empty) {
    logger.error "JQL search result is empty - SCRIPT STOPPED"
    return
}
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
    logger.info "Working with ${issue.key}"
    def customFieldValMapping = CUSTOM_FIELD_NAMES.collectEntries { [it, getCustomFieldValue(it, issue)] }
    def hasCustomFieldValue = customFieldValMapping.values().any { it != null }
    def systemFieldValMapping = SYSTEM_FIELD_NAMES.collectEntries { [it, getSystemFieldValue(it, issue)] }
    def hasSystemFieldValue = systemFieldValMapping.values().any { it != null }
    if (hasCustomFieldValue || hasSystemFieldValue) {
        logger.info "Field values are not empty"
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
        setDescription(executionUser, issue, descVal as String)
    } else logger.info "No field values detected"
}
logger.info "SCRIPT END"

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