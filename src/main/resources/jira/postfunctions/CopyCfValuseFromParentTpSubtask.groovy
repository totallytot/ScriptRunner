import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.link.IssueLinkManager
import com.atlassian.jira.user.ApplicationUser

String user = "tech_user";
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(user);
IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();

MutableIssue parentIssue = (MutableIssue) issueLinkManager.getInwardLinks(issue.getId())?.find{it.getIssueLinkType().getId() == 10100}?.getSourceObject();
CustomField bpTeam = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(18800L);
LazyLoadedOption value = (LazyLoadedOption) parentIssue.getCustomFieldValue(bpTeam);

if (value != null && issue.getCustomFieldValue(bpTeam) == null) {
    updateCfWithHistory(issue, applicationUser, bpTeam, value.getOptionId().toString());
}

private static Collection<String> updateCfWithHistory(Issue issue, ApplicationUser user, CustomField customField, String... value) {
    IssueService issueService = ComponentAccessor.getIssueService();
    IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
    issueInputParameters.addCustomFieldValue(customField.getIdAsLong(), value)
            .setSkipScreenCheck(true);
    IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(user, issue.getId(), issueInputParameters);
    if (validationResult.isValid())
    {
        IssueService.IssueResult result = issueService.update(user, validationResult);
        return result.getErrorCollection().errorMessages;
    }
    else return null;
}

