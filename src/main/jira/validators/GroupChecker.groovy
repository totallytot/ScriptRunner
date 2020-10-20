package jira.validators

import com.atlassian.jira.component.ComponentAccessor
/*
issue type creation depending on group membership
*/
def groupManager = ComponentAccessor.groupManager
def issueType = issue.issueType.name

switch (issueType) {
    case "Service Request":
        groupManager.isUserInGroup(currentUser, "Xtremax") || groupManager.isUserInGroup(currentUser, "GVT") ||
                groupManager.isUserInGroup(currentUser, "AA") || groupManager.isUserInGroup(currentUser, "CA")
        break
    case "Incident":
        groupManager.isUserInGroup(currentUser, "Xtremax") || groupManager.isUserInGroup(currentUser, "GVT") ||
                groupManager.isUserInGroup(currentUser, "AM") || groupManager.isUserInGroup(currentUser, "AA") ||
                groupManager.isUserInGroup(currentUser, "CA") || groupManager.isUserInGroup(currentUser, "Finance") ||
                groupManager.isUserInGroup(currentUser, "SIRO")
        break
    case "Problem":
    case "Change":
        groupManager.isUserInGroup(currentUser, "Xtremax") || groupManager.isUserInGroup(currentUser, "GVT")
        break
}