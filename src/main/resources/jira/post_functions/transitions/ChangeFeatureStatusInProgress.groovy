package jira.post_functions.transitions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.link.IssueLink
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.user.ApplicationUser

String user = "tech_user";
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(user);

//get link to Feature
IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
List<IssueLink> epicLinks = issueLinkManager.getInwardLinks(issue.getId());

if (epicLinks != null && epicLinks.size() > 0) {
    Issue issueFeature = null;
    epicLinks.each {
        issueFeature = it.getSourceObject();
        if (issueFeature.getIssueType().getName().equals("Roadmap Feature") && issueFeature.getKey().contains("INFRA-")) {
            transitFeature(issueFeature, applicationUser);
        }
    }
}

static Collection<String> transitFeature(Issue issueFeature, ApplicationUser applicationUser) {
    IssueService issueService = ComponentAccessor.getIssueService();
    IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();

    if (issueFeature.getAssignee() == null) issueInputParameters.setAssigneeId(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getKey());

    IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(applicationUser, issueFeature.getId(), 51, issueInputParameters);
    if (transitionValidationResult.isValid()) {
        IssueService.IssueResult transitionResult = issueService.transition(applicationUser, transitionValidationResult);
        return transitionResult.errorCollection.errorMessages;
    }
    return null;
}
