package jira.script_console.cleanup

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.workflow.WorkflowSchemeManager;

WorkflowManager workflowManager = ComponentAccessor.getWorkflowManager();
WorkflowSchemeManager workflowSchemeManager = ComponentAccessor.getWorkflowSchemeManager();
StringBuilder report = new StringBuilder();

Collection<JiraWorkflow> workflows = workflowManager.getWorkflows();
workflows.removeAll(workflowManager.getActiveWorkflows());

workflows.each {
    if (workflowSchemeManager.getSchemesForWorkflow(it).size() == 0) {
        report.append(it.getName());
        report.append(" | ")
        workflowManager.deleteWorkflow(it);
    }
}
return report.toString();






