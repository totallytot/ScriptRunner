package jira.script_console.cleanup

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenScheme;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;

IssueTypeScreenSchemeManager issueTypeScreenSchemeManager = ComponentAccessor.getIssueTypeScreenSchemeManager();
StringBuilder report = new StringBuilder();

Collection<IssueTypeScreenScheme> schemes = issueTypeScreenSchemeManager.getIssueTypeScreenSchemes();
schemes.each {
    if (issueTypeScreenSchemeManager.getProjects(it).size() == 0) {
        report.append(it.getName());
        report.append(" | ");
        issueTypeScreenSchemeManager.removeIssueTypeSchemeEntities(it);
        issueTypeScreenSchemeManager.removeIssueTypeScreenScheme(it);
    }
}

return report.toString();


