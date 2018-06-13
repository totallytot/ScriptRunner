package jira.postfunctions

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.JiraUtils;
import com.atlassian.jira.workflow.WorkflowTransitionUtil;
import com.atlassian.jira.workflow.WorkflowTransitionUtilImpl;

//for testing in order to catch the user
String user = "user";
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(user);
JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
jiraAuthenticationContext.setLoggedInUser(applicationUser);

//for testing in order to catch the issue
IssueManager issueManager = ComponentAccessor.getIssueManager();
Issue issue = issueManager.getIssueObject("TEST-26567");

CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
WorkflowTransitionUtil workflowTransitionUtil = (WorkflowTransitionUtil) JiraUtils.loadComponent(WorkflowTransitionUtilImpl.class);

CustomField epicLink = customFieldManager.getCustomFieldObject(10200L);
String epicKey =  issue.getCustomFieldValue(epicLink).toString();
MutableIssue issueEpic = issueManager.getIssueObject(epicKey);

if (issueEpic != null) {

    String originalEpicStatus = issueEpic.getStatus().getSimpleStatus().getName();

    if (originalEpicStatus.equals("To Do")) {

        Map<String, String> params = new HashMap<>();
        params.put("summary", issueEpic.summary);
        params.put("reporter", issueEpic.getReporter().getKey());

        if (issueEpic.getAssignee() == null) {
            params.put("assignee", user);
        }

        workflowTransitionUtil.setParams(params);
        workflowTransitionUtil.setIssue(issueEpic);
        workflowTransitionUtil.setUserkey(user);
        workflowTransitionUtil.setAction(51);

        if (workflowTransitionUtil.validate()) {
            workflowTransitionUtil.progress();
        }
    }
}