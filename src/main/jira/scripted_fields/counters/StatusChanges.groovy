package jira.scripted_fields.counters

import com.atlassian.jira.component.ComponentAccessor

def changeHistoryManager = ComponentAccessor.changeHistoryManager
def statusHistory = changeHistoryManager.getChangeItemsForField(issue, "status")
return statusHistory.findAll { it.fromString in ["Завершено", "Закрыто"] }?.size()