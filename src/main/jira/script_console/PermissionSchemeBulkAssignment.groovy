package jira.script_console

import com.atlassian.jira.component.ComponentAccessor

def permissionSchemeName = "Cхема Read-Only для закрытых проектов"
def permissionsSchemeManager = ComponentAccessor.permissionSchemeManager
def permissionScheme = permissionsSchemeManager.getSchemeObject(permissionSchemeName)

ComponentAccessor.projectManager.projectObjects.each {
    if (it.name.contains("АРХИВ")) {
        //Too many PermissionScheme schemes found for Project
        permissionsSchemeManager.removeSchemesFromProject(it)
        permissionsSchemeManager.addSchemeToProject(it, permissionScheme)
    }
}