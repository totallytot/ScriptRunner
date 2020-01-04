package jira.post_functions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.user.ApplicationUser

String user = "tech_user";
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(user);

Issue parentIssue = issue.getParentObject();
CustomField bpTeam = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(18800L);
LazyLoadedOption value = (LazyLoadedOption) parentIssue.getCustomFieldValue(bpTeam);

if (issue.getCustomFieldValue(bpTeam) == null)
{
    IssueService issueService = ComponentAccessor.getIssueService();
    IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
    issueInputParameters.addCustomFieldValue(bpTeam.getIdAsLong(), value.getOptionId().toString());
    IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(applicationUser, issue.getId(), issueInputParameters);
    if (validationResult.isValid()) IssueService.IssueResult result = issueService.update(applicationUser, validationResult);
}
