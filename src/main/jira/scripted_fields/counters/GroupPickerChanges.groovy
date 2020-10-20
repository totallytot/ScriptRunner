package jira.scripted_fields.counters

import com.atlassian.jira.component.ComponentAccessor

def changeHistoryManager = ComponentAccessor.changeHistoryManager
def groupHistory = changeHistoryManager.getChangeItemsForField(issue, "Группа исполнителей")
return groupHistory?.size()