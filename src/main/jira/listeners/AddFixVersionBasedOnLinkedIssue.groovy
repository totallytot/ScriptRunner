package jira.listeners

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.link.IssueLinkCreatedEvent
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.index.IssueIndexingService
import com.atlassian.jira.util.ImportUtils

def event = event as IssueLinkCreatedEvent
def linkName = event.issueLink.issueLinkType.name
def sourceIssue = event.issueLink.sourceObject as Issue //Test
def destinationIssue = event.issueLink.destinationObject as Issue//Specification Version

if (linkName == "Tests" && sourceIssue.issueType.name == "Test" && destinationIssue.issueType.name == "Specification Version") {
    def versionManager = ComponentAccessor.versionManager
    def sourceProjectFixVersionsNames = sourceIssue.projectObject.versions.collect { it.name }
    def destinationIssueFixVersionsNames = destinationIssue.fixVersions.collect { it.name }
    def versionsForUpdate = []

    //no common fix version names
    if (sourceProjectFixVersionsNames.disjoint(destinationIssueFixVersionsNames)) {
        destinationIssue.fixVersions.each { fixVersion ->
            versionsForUpdate << versionManager.createVersion(fixVersion.name, fixVersion.startDate, fixVersion.releaseDate,
                    fixVersion.description, sourceIssue.projectId, null, fixVersion.released)
        }
        //common fix version names
    } else {
        versionsForUpdate = sourceIssue.projectObject.versions?.findAll { it.name in destinationIssueFixVersionsNames }
        def versionsForCreation = destinationIssue.fixVersions.findAll { !sourceProjectFixVersionsNames.contains(it.name) }
        versionsForCreation.each { fixVersion ->
            versionsForUpdate << versionManager.createVersion(fixVersion.name, fixVersion.startDate, fixVersion.releaseDate,
                    fixVersion.description, sourceIssue.projectId, null, fixVersion.released)
        }
    }
    versionsForUpdate.addAll(sourceIssue.fixVersions)
    versionManager.updateIssueFixVersions(sourceIssue, (Collection) versionsForUpdate)
    boolean wasIndexing = ImportUtils.isIndexIssues()
    ImportUtils.setIndexIssues(true)
    ComponentAccessor.getComponent(IssueIndexingService.class).reIndex(sourceIssue)
    ImportUtils.setIndexIssues(wasIndexing)
}