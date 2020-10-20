package jira.script_console

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.project.Project
import com.atlassian.jira.project.version.Version
import com.atlassian.jira.bc.project.version.VersionService
import com.atlassian.jira.bc.project.version.VersionBuilder

//for testing in order to catch the user
String user = "tech_user"
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(user)
JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()
jiraAuthenticationContext.setLoggedInUser(applicationUser)

VersionService versionService = ComponentAccessor.getComponent(VersionService)
Project parentProject = ComponentAccessor.getProjectManager().getProjectObjByKey("ZIGZAG")
Collection<Version> parentVersions = parentProject.getVersions()
List<String> childProjetsKeys = new ArrayList()
childProjetsKeys.add("TZZ")


for (int i = 0; i<childProjetsKeys.size(); i++){
    Project childProject = ComponentAccessor.getProjectManager().getProjectObjByKey(childProjetsKeys[i])
    Collection<Version> childVersions = childProject.getVersions()

    for (int j = 0; j<parentVersions.size(); j++){
        boolean setStartDate
        boolean setReleaseDate
        boolean syncronizedVersion
        if (parentVersions[j].startDate != null) setStartDate = true
        if (parentVersions[j].releaseDate != null) setReleaseDate = true
        for (int k = 0; k<childVersions.size(); k++){
            if (parentVersions[j].name.equals(childVersions[k].name)){
                VersionBuilder versionBuilder = versionService.newVersionBuilder(childVersions[k])
                if (setStartDate) versionBuilder.startDate(parentVersions[j].startDate)
                if (setReleaseDate) versionBuilder.releaseDate(parentVersions[j].releaseDate)
                versionBuilder.released(parentVersions[j].released)
                VersionService.VersionBuilderValidationResult result = versionService.validateUpdate(applicationUser, versionBuilder)
                if (result.isValid()) versionService.update(applicationUser,result)
                syncronizedVersion = true
                break
            }
        }
        if (!syncronizedVersion){
            VersionBuilder versionBuilder = versionService.newVersionBuilder()
            versionBuilder.projectId(childProject.getId())
            versionBuilder.name(parentVersions[j].name)
            versionBuilder.startDate(parentVersions[j].startDate)
            versionBuilder.releaseDate(parentVersions[j].releaseDate)
            versionBuilder.released(parentVersions[j].released)
            VersionService.VersionBuilderValidationResult result = versionService.validateCreate(applicationUser, versionBuilder)
            if (result.isValid()) versionService.create(applicationUser,result)
        }
    }
}