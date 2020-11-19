package jira.behaviours

import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

def affectedField = getFieldById(getFieldChanged())
def affectedVal = affectedField.value
def descriptionField = getFieldById("description")

if (affectedVal instanceof String) {
    switch (affectedVal) {
        case "Performance":
            descriptionField.setFormValue("X")
            break
        case "Test":
            descriptionField.setFormValue("Y")
            break
        default:
            descriptionField.setFormValue("")
            break
    }
} else if (affectedVal instanceof List) {
    if (affectedVal == ["Performance", "Test 0"]) descriptionField.setFormValue("XX")
    else if (affectedVal == ["Test 0", "Test 1"]) descriptionField.setFormValue("YY")
    else descriptionField.setFormValue("")
}