package jira.listeners

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.link.IssueLinkTypeManager

if (event.issue.issueType.name != "Defect") return
def affectedLinkName = "PM/Defect link"
def executionUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def issue = event.issue
def issueLinkManager = ComponentAccessor.issueLinkManager
def hasPmDefectLink = issueLinkManager.getLinkCollection(issue, executionUser).linkTypes.any {
    it.name == affectedLinkName
}
if (!hasPmDefectLink) return

def outwardLinks = issueLinkManager.getOutwardLinks(issue.id)
def inwardLinks = issueLinkManager.getInwardLinks(issue.id)

// find linked problem report if inward and outward references have the same name
def linkedProblemReport = outwardLinks.findResult {
    it.issueLinkType.name == affectedLinkName &&
            it.destinationObject.issueType.name == "Problem Report" ? it.destinationObject : null
}
if (!linkedProblemReport) {
    linkedProblemReport = inwardLinks.findResult {
        it.issueLinkType.name == affectedLinkName &&
                it.sourceObject.issueType.name == "Problem Report" ? it.sourceObject : null
    }
}
// find linked change requests if inward and outward references have the same name
def linkedChangeRequests = outwardLinks.findResults {
    it.issueLinkType.name == "Relates" &&
            it.destinationObject.issueType.name == "Change Request" ? it.destinationObject : null
}
linkedChangeRequests += inwardLinks.findResults {
    it.issueLinkType.name == "Relates" &&
            it.sourceObject.issueType.name == "Change Request" ? it.sourceObject : null
}

if (linkedChangeRequests.empty) return
linkedChangeRequests = linkedChangeRequests.sort { it.created }
def latestChangeRequest = linkedChangeRequests.last()
def issueLinkTypeManager = ComponentAccessor.getComponentOfType(IssueLinkTypeManager.class) as IssueLinkTypeManager
def pmDefectLink = issueLinkTypeManager.issueLinkTypes.find { it.name == affectedLinkName }

if (!pmDefectLink) return
issueLinkManager.createIssueLink(linkedProblemReport.id, latestChangeRequest.id, pmDefectLink.id, 1, executionUser)
def pmDefectLinkForRemoval = outwardLinks.find {
    it.issueLinkType.name == affectedLinkName && it.destinationObject.issueType.name == "Problem Report"
}
if (!pmDefectLinkForRemoval) pmDefectLinkForRemoval = inwardLinks.find {
    it.issueLinkType.name == affectedLinkName && it.sourceObject.issueType.name == "Problem Report"
}
if (pmDefectLinkForRemoval) issueLinkManager.removeIssueLink(pmDefectLinkForRemoval, executionUser)