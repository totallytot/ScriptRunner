package jira.behaviours

import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

def changeReasonField = getFieldById(getFieldChanged())
def changeReasonFieldVal = changeReasonField.value as String
def descriptionField = getFieldById("description")

if (changeReasonFieldVal == "Repair") {
    descriptionField.setFormValue("Test")
}