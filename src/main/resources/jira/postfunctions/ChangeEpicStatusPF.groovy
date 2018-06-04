package jira.postfunctions

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.JiraUtils;
import com.atlassian.jira.workflow.WorkflowTransitionUtil;
import com.atlassian.jira.workflow.WorkflowTransitionUtilImpl;

CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
WorkflowTransitionUtil workflowTransitionUtil = (WorkflowTransitionUtil) JiraUtils.loadComponent(WorkflowTransitionUtilImpl.class);

JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
ApplicationUser applicationUser = jiraAuthenticationContext.getLoggedInUser();

if (issue.getIssueType().getName().equals("Story")) { //move upper

    CustomField epicLink = customFieldManager.getCustomFieldObject(10200L);
    String epicKey =  issue.getCustomFieldValue(epicLink).toString();

    IssueManager issueManager = ComponentAccessor.getIssueManager();
    MutableIssue issueEpic = issueManager.getIssueObject(epicKey);

    String originalEpicStatus = issueEpic.getStatus().getSimpleStatus().getName();

    if (originalEpicStatus.equals("To Do")) {
        String oldSummary = issueEpic.summary;
        String reporter = issueEpic.getReporter().getKey();
        def params = ["summary":oldSummary, "assignee":applicationUser.getKey(), "reporter":reporter];

        workflowTransitionUtil.setParams(params);
        workflowTransitionUtil.setIssue(issueEpic);

        if (issueEpic.getAssignee() == null) {
            workflowTransitionUtil.setUserkey(applicationUser.getKey())
        }

        workflowTransitionUtil.setAction(51);

        if (workflowTransitionUtil.validate()) {
            workflowTransitionUtil.progress()
        }
    }
}

