package jira.post_functions.transitions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.user.ApplicationUser;

/**
 * post function should be place after "Re-index an issue to keep indexes in sync with the database"
 *
 */

String user = "tech_user";
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(user);

CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
IssueManager issueManager = ComponentAccessor.getIssueManager();

CustomField epicLink = customFieldManager.getCustomFieldObject(10200L);
String epicKey =  issue.getCustomFieldValue(epicLink).toString();
MutableIssue issueEpic = issueManager.getIssueObject(epicKey);

if (issueEpic != null) {

    String originalEpicStatus = issueEpic.getStatus().getSimpleStatus().getName();

    if (originalEpicStatus.equals("In Progress") || originalEpicStatus.equals("To Do")) {
        boolean shouldChangeEpic = true;
        IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();

        int countNotClosed = 0;

        issueLinkManager.getOutwardLinks(issueEpic.getId()).each {
            if (!it.getDestinationObject().getStatus().getStatusCategory().getName().equals("Complete") && it.getIssueLinkType().getId() != 10100) {
                countNotClosed++;
            }
        }

        if (countNotClosed > 0) shouldChangeEpic = false;

        if (shouldChangeEpic) transitEpic(issueEpic, applicationUser);

    }
}

void transitEpic(Issue issue, ApplicationUser applicationUser) {
    IssueService issueService = ComponentAccessor.getIssueService();
    IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();

    if (issue.getAssignee() == null) issueInputParameters.setAssigneeId(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getKey());

    IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(applicationUser, issue.getId(), 61, issueInputParameters);
    if (transitionValidationResult.isValid()) {
        IssueService.IssueResult transitionResult = issueService.transition(applicationUser, transitionValidationResult);
    }
}
