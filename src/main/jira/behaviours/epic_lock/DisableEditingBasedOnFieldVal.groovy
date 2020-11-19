package jira.behaviours.epic_lock

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueFieldConstants
import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

log.warn "### START OF LockedForChanges ###"
log.warn "Working with ${underlyingIssue.issueType.name}"

def lockedForChangeField = getFieldById(getFieldChanged()) // LockedForChange field
def lockedForChangeFieldVal = lockedForChangeField.value as String

def field = getFieldByName("LockedForChange")
if (getFieldScreen().name == "PgM") field.setHidden(false)
else field.setHidden(true)

def systemFieldIds = IssueFieldConstants.fields.findResults {
    // apply changes to make field names similar to ids from front end
    it.name.toLowerCase().replace("_", "")
}
// system field names taken from backend differ from system field ids used in frontend for the fields below:
systemFieldIds << "fixVersions"
systemFieldIds << "versions"
systemFieldIds.remove("comment")
def customFieldsIds = ComponentAccessor.customFieldManager.getCustomFieldObjects(underlyingIssue)*.id
customFieldsIds.remove(lockedForChangeField.fieldId)
switch (lockedForChangeFieldVal) {
    case "Yes":
        systemFieldIds.each {
            getFieldById(it)?.setReadOnly(true)
        }
        customFieldsIds.each {
            getFieldById(it).setReadOnly(true)
        }
        getFieldById("attachment").setHidden(true)
        break
    default:
        systemFieldIds.each {
            getFieldById(it).setReadOnly(false)
        }
        customFieldsIds.each {
            getFieldById(it).setReadOnly(false)
        }
        break
}