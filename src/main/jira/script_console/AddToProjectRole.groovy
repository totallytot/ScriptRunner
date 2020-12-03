package jira.script_console

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.security.roles.RoleActorFactory
import com.atlassian.jira.security.roles.ProjectRoleActor

def projectManager = ComponentAccessor.projectManager
def projectRoleManager = ComponentAccessor.getComponentOfType(ProjectRoleManager)
def roleActorFactory = ComponentAccessor.getComponentOfType(RoleActorFactory)

def projectKeys = ["PKEY1", "PKEY2", "PKEY3"]
def groupNames = ["group1", "group2"]
def roleName = "role1"

def role = projectRoleManager.getProjectRole(roleName)

projectKeys.each { projectKey ->
    def project = projectManager.getProjectByCurrentKey(projectKey)
    def projectRoleActors = projectRoleManager.getProjectRoleActors(role, project)
    def newRoleActors = groupNames.collect{ groupName ->
        roleActorFactory.createRoleActor(null, role.id, project.id, ProjectRoleActor.GROUP_ROLE_ACTOR_TYPE, groupName)
    }
    projectRoleActors = projectRoleActors.addRoleActors(newRoleActors)
    projectRoleManager.updateProjectRoleActors(projectRoleActors)
}
