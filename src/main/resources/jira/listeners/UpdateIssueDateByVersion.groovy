package jira.listeners

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.project.version.Version
import com.atlassian.jira.issue.Issue
import java.text.SimpleDateFormat

Issue issue = event.issue
String user = "ext_alexk"
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(user)
JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()
jiraAuthenticationContext.setLoggedInUser(applicationUser)
Collection<Version> versions =  issue.getFixVersions()

if (versions.size()>0) {

    CustomField startDateField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12308L)
    CustomField releaseDateField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12309L)
    boolean start
    boolean release

    if (startDateField.getValue(issue)==null) start = true
    else start = false
    if (releaseDateField.getValue(issue)==null) release = true
    else release = false

    for (int i = 0; i < versions.size(); i++){
        if (start && versions[i].startDate != null){
            updateDateCfWithHistory(versions[i].getStartDate(),issue,applicationUser,startDateField)
            start = false
        }
        if (release && versions[versions.size()-i-1].releaseDate != null){
            updateDateCfWithHistory(versions[versions.size()-i-1].getReleaseDate(),issue,applicationUser,releaseDateField)
            release = false
        }
    }
}


void updateDateCfWithHistory(Date date, Issue issue, ApplicationUser user, CustomField cf) {

    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/YY")
    String stringDate  = dateFormat.format(new Date(date.getTime()))
    IssueService issueService = ComponentAccessor.getIssueService()
    IssueInputParameters issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters.addCustomFieldValue(cf.getIdAsLong(), stringDate)
    IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(user, issue.getId(), issueInputParameters)

    if (validationResult.isValid()) {
        issueService.update(user, validationResult)
    }
}