package jira.post_functions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption

def changeHistoryManager = ComponentAccessor.changeHistoryManager
def userManager = ComponentAccessor.userManager
def issueManager = ComponentAccessor.issueManager
def executionUser = userManager.getUserByName("automation")

def statusChanges = changeHistoryManager.getAllChangeItems(issue).findResults { change ->
    if (change.field == "status" && "Уточнение информации" in change.tos.values()) return change
}
def lastStatusChange = statusChanges.sort { it.created }.last()
def lastChanger = lastStatusChange.userKey
// fix for renamed users
def user = userManager.getUserByKey(lastChanger)
def mutableIssue = issueManager.getIssueObject(issue.id)

mutableIssue.setAssignee(user)
issueManager.updateIssue(executionUser, mutableIssue, EventDispatchOption.DO_NOT_DISPATCH, false)