package jira.conditions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.roles.ProjectRoleManager

final String [] ALLOWED_PROJECT_ROLES = ["Users"]

def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
projectRoleManager.getProjectRoles(currentUser, issue.projectObject)?.any {
    it.name in ALLOWED_PROJECT_ROLES
}
