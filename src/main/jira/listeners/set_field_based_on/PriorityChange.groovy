package jira.listeners.set_field_based_on

import JiraUtilHelper
import com.atlassian.jira.component.ComponentAccessor

def wasPriorityUpdated = event.changeLog.getRelated("ChildChangeItem")
        .any{ it.field.toString().equalsIgnoreCase("priority") }
if (!wasPriorityUpdated) return

def issue = event.issue
def executionUser = ComponentAccessor.userManager.getUserByName("automation")
def targetFieldName = "Ручная установка приоритета"
def conditionFieldName = "Тип обращения"

def targetFieldVal = JiraUtilHelper.getCustomFieldValue(targetFieldName, issue) as String
if (targetFieldVal == "Да") return
def conditionFieldVal = JiraUtilHelper.getCustomFieldValue(conditionFieldName, issue) as String
if (conditionFieldVal != "Системная проблема") return

def field = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
        .find { it.name == targetFieldName}
JiraUtilHelper.setSingleSelectListValue(issue, "Да", field, executionUser)