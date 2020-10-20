package jira.post_functions.create_issue

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor

def issueService = ComponentAccessor.getIssueService()
def issueInputParameters = issueService.newIssueInputParameters()
def applicationUser = ComponentAccessor.getUserManager().getUserByKey(issue.getReporter().getKey())

if (applicationUser != null) {
    issueInputParameters.setProjectId(issue.getProjectId())
            .setReporterId(applicationUser.getUsername())
            .setPriorityId(issue.getPriority().getId())
            .setSummary(issue.getSummary())
            .setIssueTypeId(issue.getIssueType().getId())
            .setSkipScreenCheck(true)
}

    def commentManager = ComponentAccessor.getCommentManager()
    def comment = commentManager.getLastComment(issue)
    String lastComment = null
    if (comment != null) {
        lastComment = comment.getBody()
        lastComment = wikiRenderer.render(lastComment, renderContext)
    }

    if (lastComment != null) issueInputParameters.setDescription(lastComment)
    else issueInputParameters.setDescription(issue.getDescription())

IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(applicationUser, issueInputParameters)
if (createValidationResult.isValid()) {
    IssueService.IssueResult createResult = issueService.create(applicationUser, createValidationResult)
}