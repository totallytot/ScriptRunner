package jira.postfunctions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.user.ApplicationUser

String user = "user";
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(user);
JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
jiraAuthenticationContext.setLoggedInUser(applicationUser);

//for testing in order to catch the issue
IssueManager issueManager = ComponentAccessor.getIssueManager();
MutableIssue issue = issueManager.getIssueObject("TEST-26790");

//take parent issue instead of sub-task and transit it
MutableIssue parentIssue;
IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
issueLinkManager.getInwardLinks(issue.getId()).each {
    if (it.getIssueLinkType().getId() == 10100) {
        parentIssue = (MutableIssue) it.getSourceObject();
        return true;
    }
}

//transit parent issue
if (parentIssue != null) {
    IssueService issueService = ComponentAccessor.getIssueService();
    IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
    if (parentIssue.getAssignee() == null) issueInputParameters.setAssigneeId(applicationUser.getKey());

    String parentIssueType = parentIssue.getIssueType().getName();
    int transitionId;

    switch (parentIssueType) {
        case "Bug":
        case "Task":
        case "Technical Debt":
        case "Story":
            transitionId = 51;
            break;
        case "Production Issue":
            transitionId = 11;
            break;
    }

    IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(applicationUser, parentIssue.getId(), transitionId, issueInputParameters);
    if (transitionValidationResult.isValid()) {
        IssueService.IssueResult transitionResult = issueService.transition(applicationUser, transitionValidationResult);
        return transitionResult.getErrorCollection().each {};
    }
}



