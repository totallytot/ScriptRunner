package jira.script_console.cleanup

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.screen.FieldScreenScheme;
import com.atlassian.jira.issue.fields.screen.FieldScreenSchemeManager;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;

FieldScreenSchemeManager fieldScreenSchemeManager = ComponentAccessor.getComponent(FieldScreenSchemeManager.class);
IssueTypeScreenSchemeManager issueTypeScreenSchemeManager = ComponentAccessor.getIssueTypeScreenSchemeManager();

StringBuilder report = new StringBuilder();
Collection<FieldScreenScheme> fieldScreenSchemes = fieldScreenSchemeManager.getFieldScreenSchemes();

fieldScreenSchemes.each {
    if (issueTypeScreenSchemeManager.getIssueTypeScreenSchemes(it).size() == 0) {
        report.append(it.getName());
        report.append(" | ");
        fieldScreenSchemeManager.removeFieldSchemeItems(it);
        fieldScreenSchemeManager.removeFieldScreenScheme(it);
    }
}
return report.toString();