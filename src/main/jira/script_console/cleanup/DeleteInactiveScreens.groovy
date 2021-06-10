package jira.script_console.cleanup

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.fields.screen.FieldScreenManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenSchemeManager;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;

FieldScreenManager fieldScreenManager = ComponentAccessor.getFieldScreenManager();
WorkflowManager workflowManager = ComponentAccessor.getWorkflowManager();
FieldScreenSchemeManager fieldScreenSchemeManager = ComponentAccessor.getComponent(FieldScreenSchemeManager.class);

Collection<FieldScreen> fieldScreens = fieldScreenManager.getFieldScreens();
Collection<JiraWorkflow> workflows = workflowManager.getWorkflows();

StringBuilder report = new StringBuilder();
int index = 0;

fieldScreens.each {fieldScreen ->
    boolean hasScreanScheme = false;
    boolean isWorkflowScreen = false;

    for (int i = 0; i < workflows.size(); i++) {
        if (workflows.getAt(i).getActionsForScreen(fieldScreen).size() > 0) {
            isWorkflowScreen = true;
            break;
        }
    }

    if (fieldScreenSchemeManager.getFieldScreenSchemes(fieldScreen).size() > 0) {
        hasScreanScheme = true;
    }

    if (!hasScreanScheme && !isWorkflowScreen) {
        report.append(fieldScreen.getName());
        report.append(" | ");
        index++;

        fieldScreen.getTabs().each {
            fieldScreenManager.removeFieldScreenLayoutItems(it);
        }
        fieldScreenManager.removeFieldScreenTabs(fieldScreen);
        fieldScreenManager.removeFieldScreen(fieldScreen.getId());
    }
}
return "All: " + index + report.toString();