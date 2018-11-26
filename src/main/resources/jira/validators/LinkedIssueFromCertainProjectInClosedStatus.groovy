package jira.validators

import com.atlassian.jira.component.ComponentAccessor

//On the project HEPROD - allow HEC Request to progress 'To Approval' Only if it has a linked HEC Request in VAL and if that one is 'Closed'

if (issue.key.contains("HEPROD-")) {
    def issueLinkManager = ComponentAccessor.issueLinkManager
    issueLinkManager.getOutwardLinks(issue.id).any {
        it.destinationObject.key.contains("HEVAL-") &&
                it.destinationObject.issueType.name == "HEC Request" &&
                it.destinationObject.status.name == "Closed"
    }
} else return true

