package jira.behaviors

import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

final LOCKED_FIELD_IDS = [
        "summary", "issuetype", "reporter", "assignee", "description", "priority", "labels",
        "issuelinks-linktype", "issuelinks-issues", "issuelinks", "components", "fixVersions",
        "versions",
        "customfield_10341", "customfield_10342", "customfield_10100", "customfield_10101",
        "customfield_10102", "customfield_10103", "customfield_10104"
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
        getFieldById("attachment").setHidden(true)
        if (underlyingIssue.issueType.name != "Epic") lockedForChangeField.setReadOnly(true)
        break
    default:
        LOCKED_FIELD_IDS.each {
            getFieldById(it).setReadOnly(false)
        }
        //if (underlyingIssue.issueType.name != "Epic") lockedForChangeField.setReadOnly(true)
        break
}