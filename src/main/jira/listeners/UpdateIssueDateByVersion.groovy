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
String user = "tech_user"
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(user)
JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext()
jiraAuthenticationContext.setLoggedInUser(applicationUser)
Collection<Version> versions =  issue.getFixVersions()

if (versions.size()>0) {

    CustomField startDateField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12308L)
    CustomField releaseDateField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12309L)
    boolean start
    boolean release
    List<Date> startDateList = new ArrayList()
    List<Date> releaseDateList = new ArrayList()
    if (startDateField.getValue(issue)==null)	start = true
    else start = false
    if (releaseDateField.getValue(issue)==null)	release = true
    else release = false
    for (int i = 0; i < versions.size(); i++){

        if (versions[i].startDate != null)      startDateList.add(versions[i].startDate)
        if (versions[i].releaseDate != null)    releaseDateList.add(versions[i].releaseDate)

    }
    startDateList.sort()
    releaseDateList.sort()
    if (start && startDateList.size()>0)        update(startDateList.get(1-1),issue,applicationUser,startDateField)
    if (release && releaseDateList.size()>0)	update(releaseDateList.get(releaseDateList.size()-1),issue,applicationUser,releaseDateField)
}


void update(Date date, Issue issue, ApplicationUser user, CustomField cf) {

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