package jira

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.user.ApplicationUser

import java.sql.Timestamp
import java.text.SimpleDateFormat

/**
 *
 https://developer.atlassian.com/server/jira/platform/performing-issue-operations/ - IssueService vs issueManager
 */

void updateDateCfWithHistory(Timestamp date, Issue issue, ApplicationUser user) {
    CustomField startDate = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(16911L);
    if (startDate.getValue(issue) == null)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/YY");
        String stringDate  = dateFormat.format(new Date(date.getTime()));

        IssueService issueService = ComponentAccessor.getIssueService();
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        issueInputParameters.addCustomFieldValue(startDate.getIdAsLong(), stringDate);
        IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(user, issue.getId(), issueInputParameters);

        if (validationResult.isValid()) {
            issueService.update(user, validationResult);
        }
    }
}

void transitIssue(Issue issueFeature, ApplicationUser applicationUser) {
    IssueService issueService = ComponentAccessor.getIssueService();
    IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();

    if (issueFeature.getAssignee() == null) issueInputParameters.setAssigneeId(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser().getKey());

    IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(applicationUser, issueFeature.getId(), 51, issueInputParameters);
    if (transitionValidationResult.isValid()) {
        IssueService.IssueResult transitionResult = issueService.transition(applicationUser, transitionValidationResult);
    }
}

void CopyOptionCF(Issue outIssue, Issue inIssue, Long outField, Long inField){


    def optionsManager = ComponentAccessor.getOptionsManager()
    String user = "tech_user";
    def applicationUser = ComponentAccessor.getUserManager().getUserByKey(user)
    def issueService  = ComponentAccessor.getIssueService()
    def issueInputParam = issueService.newIssueInputParameters()
    def customFieldManager = ComponentAccessor.getCustomFieldManager()
    def outFieldObj = customFieldManager.getCustomFieldObject(outField)
    def inFieldObj = customFieldManager.getCustomFieldObject(inField)
    List<LazyLoadedOption> outFieldValue = (List<LazyLoadedOption>) outIssue.getCustomFieldValue(outFieldObj)

    String[] val = new String[outFieldValue.size()];

    for (int i = 0; i < outFieldValue.size(); i++) {
        val[i] = outFieldValue.get(i).getOptionId().toString();
    }

    issueInputParam.addCustomFieldValue(inField, val)
    def validatedResult = issueService.validateUpdate(applicationUser, inIssue.getId(), issueInputParam)

    if (validatedResult.isValid()){

        def result  =  issueService.update(applicationUser, validatedResult)
    }
}

