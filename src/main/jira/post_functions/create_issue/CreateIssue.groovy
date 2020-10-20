package jira.post_functions.create_issue

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.user.ApplicationUser

IssueService issueService = ComponentAccessor.getIssueService()
IssueInputParameters issueInputParameters = issueService.newIssueInputParameters()
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(issue.getReporter().getKey())

if (applicationUser != null) {
    issueInputParameters.setProjectId(issue.getProjectId())
            .setReporterId(applicationUser.getUsername())
            .setPriorityId(issue.getPriority().getId())
            .setSummary(issue.getSummary())
            .setIssueTypeId(issue.getIssueType().getId())
            .setDescription("created from post-function")
            .setSkipScreenCheck(true)
}

IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(applicationUser, issueInputParameters)
if (createValidationResult.isValid()) {
    IssueService.IssueResult createResult = issueService.create(applicationUser, createValidationResult)
    return createResult
}