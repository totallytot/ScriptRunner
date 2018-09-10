package jira.scriptedfields

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.link.IssueLink

def issueLinkManager = ComponentAccessor.getIssueLinkManager()
List<IssueLink> epicLinks = issueLinkManager.getInwardLinks(issue.getId())

if (epicLinks != null && epicLinks.size() > 0) {
    Issue issueFeature = null
    epicLinks.each {
        issueFeature = it.getSourceObject()
    }
    return issueFeature
} else return null
