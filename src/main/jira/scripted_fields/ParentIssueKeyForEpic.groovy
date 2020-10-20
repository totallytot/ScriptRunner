package jira.scripted_fields

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.link.IssueLink
import com.atlassian.jira.issue.link.IssueLinkManager

if (issue.getIssueType().getName().equals("Epic")) {

    IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager()
    List<IssueLink> epicLinks = issueLinkManager.getInwardLinks(issue.getId())

    if (epicLinks != null && epicLinks.size() > 0) {
        Issue issueFeature = epicLinks[0].getSourceObject()
        return issueFeature.getKey()
    } else return null

}
