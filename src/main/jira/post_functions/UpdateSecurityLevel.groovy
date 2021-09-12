package jira.post_functions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.security.IssueSecurityLevelManager
import com.atlassian.jira.issue.security.IssueSecuritySchemeManager

final Map<String, String> DEFAULT_ISSUE_TYPE_SECURITY_LVL_MAPPING = ["Task": "Test Level 1", "Report": "Test Level 2"]

enum ProjectIssueTypeSecurityLvlMapping {
    MAPPING_00("PFTB", "Task", "Test Level 2")

    final String projectKey
    final String issueTypeName
    final String securityLevelName

    ProjectIssueTypeSecurityLvlMapping(String projectKey, String issueTypeName, String securityLevelName) {
        this.projectKey = projectKey
        this.issueTypeName = issueTypeName
        this.securityLevelName = securityLevelName
    }

    static String getSecurityLvlNameFromMapping(String projectKey, String issueTypeName) {
        this.find { it.projectKey == projectKey && it.issueTypeName == issueTypeName }?.securityLevelName
    }

    static Boolean isProjectMapping(String projectKey, String issueTypeName) {
        this.any { it.projectKey == projectKey && it.issueTypeName == issueTypeName }
    }
}

def issueSecuritySchemeManager = ComponentAccessor.getComponent(IssueSecuritySchemeManager)
def issueSecurityLevelManager = ComponentAccessor.getComponent(IssueSecurityLevelManager)
def issueManager = ComponentAccessor.issueManager

if (!(DEFAULT_ISSUE_TYPE_SECURITY_LVL_MAPPING.containsKey(issue.issueType.name)
        || ProjectIssueTypeSecurityLvlMapping.isProjectMapping(issue.projectObject.key, issue.issueType.name))) return

def securityScheme = issueSecuritySchemeManager.getSchemeFor(issue.projectObject)
def securityLevelId = null
if (ProjectIssueTypeSecurityLvlMapping.isProjectMapping(issue.projectObject.key, issue.issueType.name)) {
    def securityLevelName = ProjectIssueTypeSecurityLvlMapping.getSecurityLvlNameFromMapping(issue.projectObject.key, issue.issueType.name)
    securityLevelId = issueSecurityLevelManager.getIssueSecurityLevels(securityScheme.id).find { it.name == securityLevelName }?.id
} else {
    securityLevelId = issueSecurityLevelManager.getIssueSecurityLevels(securityScheme.id).find {
        it.name == DEFAULT_ISSUE_TYPE_SECURITY_LVL_MAPPING.get(issue.issueType.name)
    }?.id
}
if (!securityLevelId) return

issue.setSecurityLevelId(securityLevelId as Long)
issueManager.updateIssue(ComponentAccessor.jiraAuthenticationContext.loggedInUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false)