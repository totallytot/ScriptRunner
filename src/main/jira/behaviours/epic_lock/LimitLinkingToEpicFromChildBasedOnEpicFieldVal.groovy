package jira.behaviours.epic_lock

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption
import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

final String epicLockFieldName = "LockedForChange"

log.warn "### START OF EpicLockedForLinking ###"
def epicLinkField = getFieldById(getFieldChanged()) //Epic Link
def epicLinkFieldVal = epicLinkField.value as String
if (!epicLinkFieldVal) return

def epicKey = epicLinkFieldVal.substring(4)
def epicIssue = ComponentAccessor.issueManager.getIssueObject(epicKey)
def lockField = customFieldManager.customFieldObjects.find { it.name == epicLockFieldName }
def lockedFieldVal = epicIssue.getCustomFieldValue(lockField) as List<LazyLoadedOption>
if (!lockedFieldVal) return
def isEpicLocked = lockedFieldVal.any { it.value == "Yes" }
// in case current issue belongs to locked Epic do not throw the error as it will block edits
boolean isInEpic = false
def issuesInEpic = getIssuesInEpic(epicIssue)
if (!issuesInEpic.empty) {
    def keys = issuesInEpic*.key
    isInEpic = keys.any { it == underlyingIssue?.key }
}

if (isEpicLocked && !isInEpic && isFrIssueType) epicLinkField.setError("Epic ${epicKey} is locked")
else epicLinkField.clearError()

static List<Issue> getIssuesInEpic(Issue epic) {
    ComponentAccessor.issueLinkManager.getOutwardLinks(epic.id).
            findAll { it.issueLinkType.name == "Epic-Story Link" }*.destinationObject
}