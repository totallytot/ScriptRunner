package jira.postfunctions

/**
 * post function should be place after "Re-index an issue to keep indexes in sync with the database".
 *
 */

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.JiraUtils;
import com.atlassian.jira.workflow.WorkflowTransitionUtil;
import com.atlassian.jira.workflow.WorkflowTransitionUtilImpl;

CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
WorkflowTransitionUtil workflowTransitionUtil = (WorkflowTransitionUtil) JiraUtils.loadComponent(WorkflowTransitionUtilImpl.class);
IssueManager issueManager = ComponentAccessor.getIssueManager();

JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
ApplicationUser applicationUser = jiraAuthenticationContext.getLoggedInUser();

CustomField epicLink = customFieldManager.getCustomFieldObject(10200L);
String epicKey =  issue.getCustomFieldValue(epicLink).toString();
MutableIssue issueEpic = issueManager.getIssueObject(epicKey);
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

    if (shouldChangeEpic) {

        Map<String, String> params = new HashMap<>();
        params.put("summary", issueEpic.summary);
        params.put("reporter", issueEpic.getReporter().getKey());

        if (issueEpic.getAssignee() == null) {
            params.put("assignee", applicationUser.getKey());
        }

        workflowTransitionUtil.setParams(params);
        workflowTransitionUtil.setIssue(issueEpic);
        workflowTransitionUtil.setUserkey(applicationUser.getKey());
        workflowTransitionUtil.setAction(61);

        if (workflowTransitionUtil.validate()) {
            workflowTransitionUtil.progress();
        }
    }
}
