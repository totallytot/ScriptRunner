package jira.postfunctions

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

JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
ApplicationUser applicationUser = jiraAuthenticationContext.getLoggedInUser();

IssueManager issueManager = ComponentAccessor.getIssueManager();

 //use ComponentManager.loadComponent(Class, Collection)

if (issue.getIssueType().getName().equals("Story")) {

    CustomField epicLink = customFieldManager.getCustomFieldObject(10200L);
    String epicKey =  issue.getCustomFieldValue(epicLink).toString();
    MutableIssue issueEpic = issueManager.getIssueObject(epicKey);

    String originalEpicStatus = issueEpic.getStatus().getSimpleStatus().getName();

    if (originalEpicStatus.equals("In Progress") || originalEpicStatus.equals("To Do")) {

        boolean shouldChangeEpic = false;
        IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();

        int count = 0;

        issueLinkManager.getOutwardLinks(issueEpic.getId()).each{
            if (it.getDestinationObject().getStatus().getName().equals("Done")) {
                count++;
            }
        }

        if (issueLinkManager.getOutwardLinks(issueEpic.getId()).size() == count+1) shouldChangeEpic = true;

        if (shouldChangeEpic) {

            String oldSummary = issueEpic.summary;
            String assignee = applicationUser.getKey();
            String reporter = issueEpic.getReporter().getKey();
            def params = ["summary": oldSummary, "assignee": assignee, "reporter": reporter];

            workflowTransitionUtil.setParams(params);
            workflowTransitionUtil.setIssue(issueEpic);

            if (issueEpic.getAssignee() == null) {
                workflowTransitionUtil.setUserkey(applicationUser.getKey())
            }

            workflowTransitionUtil.setAction(61);

            if (workflowTransitionUtil.validate()) {
                workflowTransitionUtil.progress()
            }
        }
    }
}

