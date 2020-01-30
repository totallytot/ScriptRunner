package jira.validators

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.opensymphony.workflow.InvalidInputException
import org.apache.log4j.Level
import org.apache.log4j.Logger
import webwork.action.ActionContext

def log = Logger.getLogger("check-me")
log.setLevel(Level.DEBUG)
def fieldName = "Investigation Details Table"
def tgeCustomField = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue).find {
    it.name == fieldName
} as CustomField
log.debug("issue: " + issue.key)
log.debug("tgeCustomField:" + tgeCustomField.id)
def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser.directoryUser
Class dataManagerClass = ComponentAccessor.pluginAccessor.getClassLoader()
        .findClass("com.idalko.jira.plugins.igrid.api.data.TGEGridTableDataManager")
def tgeGridDataManager = ComponentAccessor.getOSGiComponentInstanceOfType(dataManagerClass)
List<Map<String, Object>> gridDataList = new ArrayList<Map<String, Object>>()
def result = new StringBuilder()
try {
    def gridData = tgeGridDataManager.readGridDataInEditMode(issue, tgeCustomField, null, null, 0, 10, user)
    gridDataList = gridData.values
    result.append("Grid ID=" + tgeCustomField.id + " data in edit mode: " + gridDataList)
    log.debug(result)
} catch (Exception e) {
    result.append("Grid ID=" + tgeCustomField.id + " data in edit mode cannot be retrieved: " + e.getMessage() + "\n")
    log.debug(result)
}

def params = ActionContext.request.parameterMap
def tgeValueFromScreen = params.get(tgeCustomField.id) as String
log.debug(fieldName + " (grid values from screen): " + tgeValueFromScreen)
log.debug(tgeValueFromScreen)
log.debug("gridDataList " + gridDataList.size())
if (gridDataList.empty) { // always true, except creation screen - seems bug (Jira v8.3.4; iDalko Table Grid plugin Version:1.29.11)
    def tgeValueBeforeTransition = tgeGridDataManager.readGridData(issue.id, tgeCustomField.idAsLong, null, null, 0, 10, user)
    log.debug(tgeValueBeforeTransition)
    if (!tgeValueFromScreen.contains("add")) {
        throw new InvalidInputException(tgeCustomField.id, "Add at least one record to" + fieldName + " to proceed")
    }
}

