package jira.postfunctions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.link.IssueLink
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.user.ApplicationUser

String user = "tech_user";
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(user);

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
boolean shouldtransitParent = true;

if (parentIssue != null && !parentIssue.getIssueType().getName().equals("Epic") && !parentIssue.getIssueType().getName().equals("Roadmap Feature")) {

    //check if there is any not closed sub-task
    int countNotClosed = 0;
    List<IssueLink> outwardLinks = issueLinkManager.getOutwardLinks(parentIssue.getId());
    outwardLinks.each {
        if (it.getIssueLinkType().getId() == 10100 && !it.getDestinationObject().getStatus().getStatusCategory().getName().equals("Complete")) {
            countNotClosed++;
        }
    }

    if (countNotClosed > 0) shouldtransitParent = false;

    if (shouldtransitParent) {
        IssueService issueService = ComponentAccessor.getIssueService();
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        if (parentIssue.getAssignee() == null) issueInputParameters.setAssigneeId(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getKey());

        String parentIssueType = parentIssue.getIssueType().getName();
        int transitionId;

        switch (parentIssueType) {
            case "Bug":
            case "Task":
            case "Technical Debt":
                transitionId = 101;
                break;
            case "Production Issue":
            case "Story":
                transitionId = 61;
                break;
            case "Framework":
                transitionId = 21;
                break;
        }

        IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(applicationUser, parentIssue.getId(), transitionId, issueInputParameters);
        if (transitionValidationResult.isValid()) {
            IssueService.IssueResult transitionResult = issueService.transition(applicationUser, transitionValidationResult);
            return transitionResult.getErrorCollection().each {};
        }
    }
}