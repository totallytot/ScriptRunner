package jira.listeners

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.link.IssueLinkCreatedEvent
import com.atlassian.jira.issue.Issue

def event = event as IssueLinkCreatedEvent
def linkName = event.issueLink.issueLinkType.name
def sourceIssue = event.issueLink.sourceObject as Issue //Test
def destinationIssue = event.issueLink.destinationObject as Issue//SV

if (linkName == "Tests" && sourceIssue.issueType.name == "Test" && destinationIssue.issueType.name == "Specification Version") {
    def versionManager = ComponentAccessor.versionManager
    def sourceFixVersions = sourceIssue.fixVersions
    def destinationFixVersions = destinationIssue.fixVersions
    def sourceProjectFixVersions = sourceIssue.projectObject.versions
    def sourceProjectFixVersionsNames = sourceIssue.projectObject.versions.collect{it.name}
    def destinationIssueFixVersionsNames = destinationIssue.fixVersions.collect {it.name}

    //no common fix version names
    if (sourceProjectFixVersionsNames.disjoint(destinationIssueFixVersionsNames)) {
        def newVersions = []
        destinationFixVersions.each{ fixVersion ->
            newVersions << versionManager.createVersion(fixVersion.name, fixVersion.startDate, fixVersion.releaseDate,
                    fixVersion.description, sourceIssue.projectId, null, fixVersion.released)
        }
        newVersions.addAll(sourceFixVersions)
        versionManager.updateIssueFixVersions(sourceIssue, newVersions)
        //common fix version names
    } else {
        def commonVersionsByName = []
        commonVersionsByName = sourceProjectFixVersions?.findAll{ it.name in destinationIssueFixVersionsNames }
        def versionsForCreation  = destinationFixVersions.findAll{ !sourceProjectFixVersionsNames.contains(it.name) }

        versionsForCreation.each{ fixVersion ->
            commonVersionsByName << versionManager.createVersion(fixVersion.name, fixVersion.startDate, fixVersion.releaseDate,
                    fixVersion.description, sourceIssue.projectId, null, fixVersion.released)
        }
        commonVersionsByName.addAll(sourceFixVersions)
        versionManager.updateIssueFixVersions(sourceIssue, commonVersionsByName)
    }
}