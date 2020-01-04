package jira.post_functions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.util.IssueChangeHolder

/*
Updates multi group picker fields depending on issue type
*/
def cfManager = ComponentAccessor.customFieldManager
def groupManager = ComponentAccessor.groupManager
def readCf = cfManager.getCustomFieldObject(11102)
def updateCf = cfManager.getCustomFieldObject(11103)
def issueType = issue.issueType.name
IssueChangeHolder changeHolder = new DefaultIssueChangeHolder()

switch (issueType) {
    case "Incident":
        def groups = [groupManager.getGroup("Xtremax"), groupManager.getGroup("GVT"), groupManager.getGroup("AM"),
                      groupManager.getGroup("AA"), groupManager.getGroup("CA"), groupManager.getGroup("Finance"),
                      groupManager.getGroup("SIRO")]
        readCf.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(readCf), groups), changeHolder)
        updateCf.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(updateCf), groups), changeHolder)
        break
    case "Problem":
    case "Change":
        def groups = [groupManager.getGroup("Xtremax"), groupManager.getGroup("GVT")]
        readCf.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(readCf), groups), changeHolder)
        updateCf.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(updateCf), groups), changeHolder)
        break
    case "Service Request":
        def readGroups = [groupManager.getGroup("Xtremax"), groupManager.getGroup("GVT"), groupManager.getGroup("AM"),
                          groupManager.getGroup("AA"), groupManager.getGroup("CA"), groupManager.getGroup("Finance"),
                          groupManager.getGroup("SIRO")]
        readCf.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(readCf), readGroups), changeHolder)
        def updateGroups = [groupManager.getGroup("Xtremax"), groupManager.getGroup("GVT"), groupManager.getGroup("AM"),
                            groupManager.getGroup("AA"), groupManager.getGroup("CA")]
        updateCf.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(updateCf), updateGroups), changeHolder)
        break
}