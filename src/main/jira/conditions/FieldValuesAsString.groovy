package jira.conditions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue

def requestTypeValue = getCustomFieldValue("Request Type", issue) as String
def processTypeValue = getCustomFieldValue("Process Type", issue) as String

return requestTypeValue == "New" && processTypeValue == "Analysis"

static def getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue).find { it.name == customFieldName }.getValue(issue)
}