package jira.script_console

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.roles.ProjectRoleActors
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.security.roles.RoleActor

def projectRoleManager = ComponentAccessor.getComponentOfType(ProjectRoleManager.class)
def project = ComponentAccessor.projectManager.getProjectByCurrentKey("RAD")
def projectRole = projectRoleManager.getProjectRole("Verificators")
def projectRoleActors = projectRoleManager.getProjectRoleActors(projectRole, project)
def userRoleActorsForRemoval = projectRoleActors.roleActors.findAll { RoleActor roleActor ->
    roleActor.type == "atlassian-user-role-actor"
}
def updatedActors = projectRoleActors.removeRoleActors(userRoleActorsForRemoval)
projectRoleManager.updateProjectRoleActors(updatedActors as ProjectRoleActors)