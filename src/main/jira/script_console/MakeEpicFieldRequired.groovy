package jira.script_console

import com.atlassian.jira.component.ComponentAccessor

// Make Epic Link Field Required Without SR Behaviours
def fieldLayoutManager = ComponentAccessor.fieldLayoutManager

def projectId = ComponentAccessor.projectManager.getProjectObjByName("Test Epic Link").id
def issueTypeId = ComponentAccessor.constantsManager.allIssueTypeObjects.find { it.name == "Task"}.id
def fieldLayout = fieldLayoutManager.getFieldLayout(projectId, issueTypeId)

def editableFieldLayout = fieldLayoutManager.getEditableFieldLayout(fieldLayout.id)
def epicLinkField = ComponentAccessor.customFieldManager.customFieldObjects.find { it.name == "Epic Link" }
def fieldLayoutItem = editableFieldLayout.getFieldLayoutItem(epicLinkField)

editableFieldLayout.makeRequired(fieldLayoutItem)
fieldLayoutManager.storeEditableFieldLayout(editableFieldLayout)