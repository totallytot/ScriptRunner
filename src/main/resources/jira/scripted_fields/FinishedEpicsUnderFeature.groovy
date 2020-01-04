package jira.scripted_fields

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue

def issue = issue as MutableIssue
if (issue.issueType.name == "Roadmap Feature") {
    def linker = ComponentAccessor.issueLinkManager
    return linker.getOutwardLinks(issue.id).findAll{ it.destinationObject.issueType.name == "Epic" &&
            it.destinationObject.status.statusCategory.name == "Complete"}.size()
}