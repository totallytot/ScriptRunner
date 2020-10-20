package jira.behaviors

import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

final LOCKED_FIELD_IDS = [
        "summary", "reporter", "assignee", "description", "issuelinks-linktype", "issuelinks-issues", "issuelinks",
        "issuetype", "customfield_10341", "customfield_10342"
]
log.info "### START OF EpicLockedForChanges ###"
log.info "Working with ${underlyingIssue.issueType.name}"
def lockedForChangeField = getFieldById(getFieldChanged())
def lockedForChangeFieldVal = lockedForChangeField.value as String
log.info "lockedForChangeFieldVal: ${lockedForChangeFieldVal}"
switch (lockedForChangeFieldVal) {
    case "Yes":
        LOCKED_FIELD_IDS.each {
            getFieldById(it).setReadOnly(true)
        }
        break
    default:
        LOCKED_FIELD_IDS.each {
            getFieldById(it).setReadOnly(false)
        }
        break
}