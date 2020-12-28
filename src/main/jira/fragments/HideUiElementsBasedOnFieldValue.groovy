package jira.fragments

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption

//If the condition is true then the UI element will be displayed.

final String lockedField = "LockedForChange"
final List allowed_issue_types = ["Epic", "FR"]

if (issue.issueType.name in allowed_issue_types) {
    def customFieldManager = ComponentAccessor.customFieldManager
    def lockField = customFieldManager.customFieldObjects.find { it.name == lockedField }
    def lockedFieldVal = issue.getCustomFieldValue(lockField) as List<LazyLoadedOption>
    if (lockedFieldVal?.any { it.value == "Yes" }) return false
    else true
} else true