package jira.listeners

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.history.ChangeItemBean
/*
Was used in listener "Fires an event when condition is true"
 */
//def issue = ComponentAccessor.issueManager.getIssueObject("BTP-29") as MutableIssue
List<ChangeItemBean> history = ComponentAccessor.getChangeHistoryManager().getChangeItemsForField(issue, "assignee")

if (history) {
    def previousAssignee = history.get(history.size() - 1).getFrom()
    def currentAssignee = history.get(history.size() - 1).getTo()
    return previousAssignee == "viktar" && currentAssignee != "viktar"
}