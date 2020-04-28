package jira.post_functions.transitions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.link.IssueLink
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.user.ApplicationUser

if (issue.getIssueType().getName().equals("Epic")) {

    String user = "tech_user";
    ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(user);

    //get links leading to Feature
    IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
    List<IssueLink> epicLinks = issueLinkManager.getInwardLinks(issue.getId());

    //get issue Feature
    if (epicLinks != null && epicLinks.size() > 0) {
        Issue issueFeature = null;
        epicLinks.each {
            issueFeature = it.getSourceObject();
            if (issueFeature.getIssueType().getName().equals("Roadmap Feature") && issueFeature.getKey().contains("INFRA-")) {
                changeFeature(issueFeature, applicationUser);
            }
        }
    }
}

void changeFeature(Issue issueFeature, ApplicationUser applicationUser) {

    //get links from feature
    List<IssueLink> featureLinks = ComponentAccessor.getIssueLinkManager().getOutwardLinks(issueFeature.getId());
    boolean shouldChangeFeature = true;

    int count = 0;

    featureLinks.each {
        if (it.getIssueLinkType().getId() != 10100 && !it.getDestinationObject().getStatus().getStatusCategory().getName().equals("Complete")) {
            count++;
        }
    }

    if (count > 0) shouldChangeFeature = false;

    if (shouldChangeFeature) {
        IssueService issueService = ComponentAccessor.getIssueService();
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();

        if (issueFeature.getAssignee() == null) issueInputParameters.setAssigneeId(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getKey());

        IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(applicationUser, issueFeature.getId(), 61, issueInputParameters);
        if (transitionValidationResult.isValid()) {
            IssueService.IssueResult transitionResult = issueService.transition(applicationUser, transitionValidationResult);
        }
    }
}