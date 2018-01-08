import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.workflow.WorkflowService;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.JiraWorkflow;

String oldName="Workflow 1.1";
String newName = "Workflow 1";

ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
JiraServiceContextImpl jiraServiceContextImpl = new JiraServiceContextImpl(currentUser);
WorkflowService ws = ComponentAccessor.getComponent(WorkflowService.class);
JiraWorkflow workflow = ws.getWorkflow(jiraServiceContextImpl, oldName);
ws.updateWorkflowNameAndDescription(jiraServiceContextImpl, workflow, newName, workflow.getDescription());
