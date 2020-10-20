package jira.post_functions

import com.atlassian.jira.bc.project.component.ProjectComponent
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueFactory
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.link.DefaultIssueLinkManager
import com.atlassian.jira.project.Project
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.user.ApplicationUser

//script for post-function was used in JIRA 7.5
//checks cf of checkbox type, if there are checked values that are equal to project keys - clones issue to this projects with cfs values,
//sets components if they are the same on source-target projects, and makes issue link "Cloners"

JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()
ApplicationUser applicationUser = jiraAuthenticationContext.getLoggedInUser()

CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
CustomField checkBoxCF = customFieldManager.getCustomFieldObject(10200L)//checkbox CF

List <LazyLoadedOption> values = (List<LazyLoadedOption>) issue.getCustomFieldValue(checkBoxCF)

for (LazyLoadedOption option : values) {
    Project project = ComponentAccessor.projectManager.getProjectByCurrentKeyIgnoreCase(option.getValue())

    IssueFactory issueFactory = ComponentAccessor.getIssueFactory()
    MutableIssue clone = issueFactory.cloneIssue(issue)

    List<CustomField> allCustomFields = ComponentAccessor.getCustomFieldManager().getCustomFieldObjects(issue)
    IssueManager issueManager = ComponentAccessor.getIssueManager()

    for (CustomField cf : allCustomFields) {
        clone.setCustomFieldValue(cf, issue.getCustomFieldValue(cf))
    }

    List<ProjectComponent> targetComponents = project.getComponents()
    List<ProjectComponent> sourceComponents = issue.getComponents()
    clone.setProjectId(project.getId())

    //set project components to target issue
    if (sourceComponents.size() > 0 && targetComponents.size() > 0) {
        Map<String, ProjectComponent> targetComponetsMap = new HashMap<>()
        for (ProjectComponent projectComponent : targetComponents) targetComponetsMap.put(projectComponent.getName(), projectComponent)

        Map<String, ProjectComponent> sourceComponentsMap = new HashMap<>()
        for (ProjectComponent projectComponent : sourceComponents) sourceComponentsMap.put(projectComponent.getName(), projectComponent)

        ArrayList<ProjectComponent> tList = new ArrayList<>()
        for (Map.Entry<String, ProjectComponent> entry : sourceComponentsMap.entrySet()) {
            if (targetComponetsMap.containsKey(entry.getKey())) {
                tList.add(targetComponetsMap.get(entry.getKey()))
            }
        }
        clone.setComponent(tList)
    }
    //put issue to DB
    Issue clonedIssue = issueManager.createIssueObject(applicationUser, clone)
    DefaultIssueLinkManager issueLink = ComponentAccessor.issueLinkManager
    issueLink.createIssueLink(clonedIssue.getId(), issue.getId(), 10001, 1, applicationUser) //10001 - id of clone link
}