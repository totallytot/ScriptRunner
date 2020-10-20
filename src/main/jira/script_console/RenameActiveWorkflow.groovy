package jira.script_console

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.workflow.WorkflowService;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.JiraWorkflow;

/**
 * After running the script, go to workflow scheme with affected workflow and updated scheme by adding any change
 * (assign any issue type to any workflow in the scheme and revert the change => Publish). This will trigger changes
 * for old issues, which use affected workflow. Otherwise, workflow buttons will disappear from issue view on all issues,
 * that use affected workflow and had been created prior to workflow renaming.
 */

String oldName="Workflow 1.1";
String newName = "Workflow 1";

ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
JiraServiceContextImpl jiraServiceContextImpl = new JiraServiceContextImpl(currentUser);
WorkflowService ws = ComponentAccessor.getComponent(WorkflowService.class);
JiraWorkflow workflow = ws.getWorkflow(jiraServiceContextImpl, oldName);
ws.updateWorkflowNameAndDescription(jiraServiceContextImpl, workflow, newName, workflow.getDescription());
