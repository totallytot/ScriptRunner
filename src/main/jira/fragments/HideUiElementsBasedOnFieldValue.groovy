package jira.fragments

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption

//If the condition is true then the UI element will be displayed.

final String LOCK_FIELD_NAME = "LockedForChange"
final List AFFECTED_ISSUE_TYPE_NAMES = ["Epic", "FR"]

if (issue.issueType.name in AFFECTED_ISSUE_TYPE_NAMES) {
    def customFieldManager = ComponentAccessor.customFieldManager
    def lockField = customFieldManager.customFieldObjects.find { it.name == LOCK_FIELD_NAME }
    def lockedFieldVal = issue.getCustomFieldValue(lockField) as List<LazyLoadedOption>
    if (lockedFieldVal?.any { it.value == "Yes" }) return false
    else true
} else true