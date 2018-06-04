package jira.postfunctions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.link.IssueLink
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.user.ApplicationUser

if (issue.getIssueType().getName().equals("Epic")) {

    JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
    ApplicationUser applicationUser = jiraAuthenticationContext.getLoggedInUser();

    //get links leading to Feature
    IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();
    List<IssueLink> epicLinks = issueLinkManager.getInwardLinks(issue.getId());

    //get issue Feature
    Issue issueFeature = null;
    epicLinks.each {
        if (it.getSourceObject().getIssueType().getName().equals("Roadmap Feature")) {
            issueFeature = it.getSourceObject();
        }
    }

    if (issueFeature != null) {

        //get links from feature
        List<IssueLink> featureLinks = issueLinkManager.getOutwardLinks(issueFeature.getId());
        boolean shouldChangeFeature = false;

        int count = 0;

        featureLinks.each {
            if (it.getDestinationObject().getStatus().getName().equals("Done")) {
                count++;
            }
        }

        if (featureLinks.size() == count+1) shouldChangeFeature = true;

        if (shouldChangeFeature) {
            IssueService issueService = ComponentAccessor.getIssueService();
            IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();

            if (issueFeature.getAssignee() == null) issueInputParameters.setAssigneeId(applicationUser.toString())

            IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(applicationUser, issueFeature.getId(), 61, issueInputParameters);
            if (transitionValidationResult.isValid()) {
                IssueService.IssueResult transitionResult = issueService.transition(applicationUser, transitionValidationResult);
            }
        }
    }
}
