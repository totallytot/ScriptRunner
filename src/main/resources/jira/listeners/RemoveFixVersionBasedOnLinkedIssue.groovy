package jira.listeners

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.link.IssueLinkDeletedEvent
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.index.IssueIndexingService
import com.atlassian.jira.util.ImportUtils

def event = event as IssueLinkDeletedEvent
def linkName = event.issueLink.issueLinkType.name
def sourceIssue = event.issueLink.sourceObject as MutableIssue //Test
def destinationIssue = event.issueLink.destinationObject as MutableIssue//Specification Version

if (linkName == "Tests" && sourceIssue.issueType.name == "Test" && destinationIssue.issueType.name == "Specification Version") {
    def commonFixVersionsNames = sourceIssue.fixVersions.collect {
        it.name
    }.intersect(destinationIssue.fixVersions.collect { it.name })
    def versionsForUpdate = sourceIssue.fixVersions
    versionsForUpdate.removeAll {it.name in commonFixVersionsNames}
    ComponentAccessor.versionManager.updateIssueFixVersions(sourceIssue, (Collection) versionsForUpdate)
    boolean wasIndexing = ImportUtils.isIndexIssues()
    ImportUtils.setIndexIssues(true)
    ComponentAccessor.getComponent(IssueIndexingService.class).reIndex(sourceIssue)
    ImportUtils.setIndexIssues(wasIndexing)
}