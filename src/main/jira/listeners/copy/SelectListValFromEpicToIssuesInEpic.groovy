package jira.listeners.copy

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption

final String LOCK_FIELD_NAME = "LockedForChange"
final List ALLOWED_ISSUE_TYPES_IN_EPIC = ["FR"]

//condition
def issue = event.issue
if (issue.issueType.name != "Epic") return
def changeItems = event.changeLog.getRelated("ChildChangeItem")
if (!changeItems?.any { it.field.toString().equalsIgnoreCase(LOCK_FIELD_NAME) }) return

def issueManager = ComponentAccessor.issueManager
def customFieldManager = ComponentAccessor.customFieldManager
def executionUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def lockField = customFieldManager.customFieldObjects.find { it.name == LOCK_FIELD_NAME }

def lockedFieldVal = issue.getCustomFieldValue(lockField) as List<LazyLoadedOption>
def issuesInEpic = getIssuesInEpic(issue)
if (issuesInEpic.empty) return

def affectedIssueKeys = issuesInEpic?.findResults { if (it.issueType.name in ALLOWED_ISSUE_TYPES_IN_EPIC) return it.key }
affectedIssueKeys.each {
    def mutableIssue = issueManager.getIssueObject(it)
    mutableIssue.setCustomFieldValue(lockField, lockedFieldVal)
    issueManager.updateIssue(executionUser, mutableIssue, EventDispatchOption.DO_NOT_DISPATCH, false)
}

static List<Issue> getIssuesInEpic(Issue epic) {
    ComponentAccessor.issueLinkManager.getOutwardLinks(epic.id).
            findAll { it.issueLinkType.name == "Epic-Story Link" }*.destinationObject
}