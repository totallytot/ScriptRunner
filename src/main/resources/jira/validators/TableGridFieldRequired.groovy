package jira.validators

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.opensymphony.workflow.InvalidInputException
/**
 * docs: https://docs.idalko.com/tgng/pages/viewpage.action?pageId=26183994
 */
def tgeCustomField = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue).find {
    it.name == "Immediate actions (corrections)"
} as CustomField
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser.directoryUser
Class dataManagerClass = ComponentAccessor.pluginAccessor.classLoader
        .findClass("com.idalko.jira.plugins.igrid.api.data.TGEGridTableDataManager")
def tgeGridDataManager = ComponentAccessor.getOSGiComponentInstanceOfType(dataManagerClass)
List<Map<String, Object>> gridDataList = new ArrayList<Map<String, Object>>()
def result = new StringBuilder()
try {
    def gridData = tgeGridDataManager.readGridDataInEditMode(issue, tgeCustomField, null, null, 0, 10, user)
    gridDataList = gridData.values
    result.append("Grid ID=" + tgeCustomField.id + " data in edit mode: " + gridDataList)
} catch (Exception e) {
    result.append("Grid ID=" + tgeCustomField.id + " data in edit mode cannot be retrieved: " + e.message + "\n")
}
if (gridDataList.empty)
    throw new InvalidInputException(tgeCustomField.id, "Add at least one record to 'Immediate actions (corrections)' to proceed")