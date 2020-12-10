package jira.behaviours.epic_lock

import com.atlassian.jira.component.ComponentAccessor
import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

def issueTypeField = getFieldById(getFieldChanged()) // issue type field
def issueTypeFieldVal = issueTypeField.value as String
def frIssueTypeId = ComponentAccessor.constantsManager.allIssueTypeObjects.find {
    it.name == "FR"
}?.id

if (issueTypeFieldVal != frIssueTypeId) {
    def epicLinkField = getFieldByName("Epic Link")
    epicLinkField.clearError()
}