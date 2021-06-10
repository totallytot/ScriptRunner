package jira.script_console.cleanup

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;

IssueTypeSchemeManager issueTypeSchemeManager = ComponentAccessor.getIssueTypeSchemeManager();
List<FieldConfigScheme> fieldConfigSchemes = issueTypeSchemeManager.getAllSchemes();
StringBuilder report = new StringBuilder();

int i = 0;

fieldConfigSchemes.each {

    if (!it.getName().equals("Default Issue Type Scheme") && it.getAssociatedProjectObjects().size() == 0) {
        report.append(it.getName());
        report.append(" | ");
        i++;
        issueTypeSchemeManager.deleteScheme(it);
    }
}
return "All:"+ i + " " + report.toString();