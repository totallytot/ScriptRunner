package jira.startup

import com.atlassian.jira.component.ComponentAccessor

/**
 * Make Epic Link Field Required Without SR Behaviours
 * Field Configurations Ids: jira_base_url/secure/admin/ViewFieldLayouts.jspa
 * Does not work with Default Field Configuration
 */

def affectedFieldLayoutIds = [10400, 10500]

def epicLinkField = ComponentAccessor.customFieldManager.customFieldObjects.find { it.name == "Epic Link" }
def fieldLayoutManager = ComponentAccessor.fieldLayoutManager

affectedFieldLayoutIds.each { fieldLayoutId  ->
    def editableFieldLayout = fieldLayoutManager.getEditableFieldLayout(fieldLayoutId)
    def fieldLayoutItem = editableFieldLayout.getFieldLayoutItem(epicLinkField)
    if (editableFieldLayout && fieldLayoutItem) {
        editableFieldLayout.makeRequired(fieldLayoutItem)
        fieldLayoutManager.storeEditableFieldLayout(editableFieldLayout)
    }
}