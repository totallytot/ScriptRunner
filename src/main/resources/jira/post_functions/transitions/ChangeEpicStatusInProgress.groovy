package jira.post_functions.transitions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.user.ApplicationUser;

String user = "tech_user";
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(user);

CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
CustomField epicLink = customFieldManager.getCustomFieldObject(10200L);
String epicKey = issue.getCustomFieldValue(epicLink).toString();

IssueManager issueManager = ComponentAccessor.getIssueManager();
MutableIssue issueEpic = issueManager.getIssueObject(epicKey);

if (issueEpic != null) {

    String originalEpicStatus = issueEpic.getStatus().getSimpleStatus().getName();

    if (originalEpicStatus.equals("To Do")) {
        transitEpic(issueEpic, applicationUser);
    }
}

void transitEpic(Issue issue, ApplicationUser applicationUser) {
    IssueService issueService = ComponentAccessor.getIssueService();
    IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();

    if (issue.getAssignee() == null) issueInputParameters.setAssigneeId(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getKey());

    IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(applicationUser, issue.getId(), 51, issueInputParameters);
    if (transitionValidationResult.isValid()) {
        IssueService.IssueResult transitionResult = issueService.transition(applicationUser, transitionValidationResult);
    }
}