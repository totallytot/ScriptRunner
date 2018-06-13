package jira.postfunctions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.link.IssueLink
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.user.ApplicationUser

//for testing in order to catch the user
String user = "user";
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(user);
JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
jiraAuthenticationContext.setLoggedInUser(applicationUser);

//for testing in order to catch the issue
IssueManager issueManager = ComponentAccessor.getIssueManager();

//get EPIC
MutableIssue issue = issueManager.getIssueObject("TEST-26786");

//get link to Feature
IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
List<IssueLink> epicLinks = issueLinkManager.getInwardLinks(issue.getId());

//get Feature
Issue issueFeature = null;
epicLinks.each {
    if (it.getSourceObject().getIssueType().getName().equals("Roadmap Feature")) {
        issueFeature = it.getSourceObject();
        return true;
    }
}

if (issueFeature != null) {
    IssueService issueService = ComponentAccessor.getIssueService();
    IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();

    if (issueFeature.getAssignee() == null) issueInputParameters.setAssigneeId(applicationUser.getKey());

    IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(applicationUser, issueFeature.getId(), 51, issueInputParameters);
    if (transitionValidationResult.isValid()) {
        IssueService.IssueResult transitionResult = issueService.transition(applicationUser, transitionValidationResult);
        return transitionResult.errorCollection.errorMessages.each {}
    }
}
