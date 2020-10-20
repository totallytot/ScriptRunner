package jira.post_functions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField

CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
CustomField storyPointsField = customFieldManager.getCustomFieldObject(10307L) //story point field

String sp = issue.getCustomFieldValue(storyPointsField)

if (sp == null || sp.isEmpty()) sp = "0"

double spD = Double.parseDouble(sp)

switch (spD) {
    case(0):
    case(1):
    case(2):
    case(3):
        issue.setPriorityId("5")
        break
    case(4):
    case(5):
    case(6):
    case(7):
    case(8):
        issue.setPriorityId("4")
        break
}
