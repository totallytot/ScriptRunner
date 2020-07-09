package jira.listeners

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.link.IssueLinkTypeManager

if (event.issue.issueType.name != "Defect") return
def issue = event.issue

def issueLinkManager = ComponentAccessor.issueLinkManager
def outwardsLinks = issueLinkManager.getOutwardLinks(issue.id)

def linkedProblemReport = outwardsLinks.findResult {
    it.issueLinkType.name == "PM/Defect link" &&
            it.destinationObject.issueType.name == "Problem Report" ? it.destinationObject : null
}
if (!linkedProblemReport) return

def linkedChangeRequests = outwardsLinks.findResults {
    it.issueLinkType.name == "Relates" &&
            it.destinationObject.issueType.name == "Change Request" ? it.destinationObject : null
}
if (linkedChangeRequests.empty) return
linkedChangeRequests.groupBy { it.created }

def latestChangeRequest = linkedChangeRequests.first()
def executionUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def issueLinkTypeManager = ComponentAccessor.getComponentOfType(IssueLinkTypeManager.class) as IssueLinkTypeManager
def pmDefectLink = issueLinkTypeManager.issueLinkTypes.find { it.name == "PM/Defect link" }

if (!pmDefectLink) return
issueLinkManager.createIssueLink(linkedProblemReport.id, latestChangeRequest.id, pmDefectLink.id, 1, executionUser)
def pmDefectLinkForRemoval = outwardsLinks.find {
    it.issueLinkType.name == "PM/Defect link" && it.destinationObject.issueType.name == "Problem Report"
}
if (!pmDefectLinkForRemoval) return
issueLinkManager.removeIssueLink(pmDefectLinkForRemoval, executionUser)