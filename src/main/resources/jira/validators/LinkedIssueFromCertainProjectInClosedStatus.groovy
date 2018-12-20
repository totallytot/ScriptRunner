package jira.validators

import com.atlassian.jira.component.ComponentAccessor

//On the project HEPROD - allow HEC Request to progress 'To Approval' Only if it has a linked HEC Request.
//Link type should be 'VAL HECReq reference'. The behavior should be so that issues linked in this way are 'Closed'.

if (issue.key.contains("HEPROD-")) {
    def VALHECReqReference = ComponentAccessor.issueLinkManager.getOutwardLinks(issue.id).findAll {
        it.linkTypeId == 10400 && it.destinationObject.key.contains("HEVAL-") &&
                it.destinationObject.issueType.name == "HEC Request"
    }
        if (VALHECReqReference) VALHECReqReference.every {it.destinationObject.status.name == "Closed"}
        else false
} else true