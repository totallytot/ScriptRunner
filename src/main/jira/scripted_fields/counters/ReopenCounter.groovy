package jira.scripted_fields.counters

import com.atlassian.jira.component.ComponentAccessor

def changeHistoryManager = ComponentAccessor.changeHistoryManager
def statusChangeItemBean = changeHistoryManager.getChangeItemsForField(issue, "status")
int reopenedCount = statusChangeItemBean.findAll { it.toString == "Reopened" }?.size()
int undoReopenCount = statusChangeItemBean.findAll { it.toString == "Undo Reopen" }?.size()
return reopenedCount - undoReopenCount
