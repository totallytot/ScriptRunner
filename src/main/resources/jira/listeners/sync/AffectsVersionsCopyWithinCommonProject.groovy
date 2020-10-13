package jira.listeners.sync

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.user.ApplicationUser

def issue = event.issue as MutableIssue
def affectedLinkName = "PMS/DEV link"
def allowedIssueTypes = ["Problem Report", "Defect", "Change Request"]
def executionUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def changeItems = event.changeLog.getRelated("ChildChangeItem")
def issueLinkManager = ComponentAccessor.issueLinkManager

log.warn "Working with ${issue.key}"
// condition
if (!allowedIssueTypes.contains(issue.issueType.name)) return
log.warn "Allowed issue type"
def wasAffectsVersionChanged = changeItems.any { it.get("field").toString().equalsIgnoreCase("version") }
if (!wasAffectsVersionChanged) return
log.warn "Affects Version Changed"
def hasAffectedLink = issueLinkManager.getLinkCollection(issue, executionUser).linkTypes.any {
    it.name == affectedLinkName
}
if (!hasAffectedLink) return
log.warn "Link exists"
// find linked issue if inward and outward references have the same name
def inwardIssues = issueLinkManager.getInwardLinks(issue.id).findResults {
    it.issueLinkType.name == affectedLinkName && it.destinationObject.key == issue.key ? it.sourceObject : null
}
def outwardIssues = issueLinkManager.getOutwardLinks(issue.id).findResults {
    it.issueLinkType.name == affectedLinkName && it.sourceObject.key == issue.key ? it.destinationObject : null
}
def affectedIssues = inwardIssues + outwardIssues
if (!affectedIssues) return
log.warn "Condition passed"

def sourceVersionIds = issue.affectedVersions*.id
affectedIssues.each {
    setAffectedVersions(executionUser, it as MutableIssue, sourceVersionIds as Long[])
}

static def setAffectedVersions(ApplicationUser executionUser, MutableIssue affectedIssue, Long... versionIds) {
    def issueService = ComponentAccessor.issueService
    def issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters.with {
        setSkipScreenCheck(true)
        setAffectedVersionIds(versionIds)
    }
    IssueService.UpdateValidationResult validationResult = issueService
            .validateUpdate(executionUser, affectedIssue.id, issueInputParameters)
    if (validationResult.valid) issueService.update(executionUser, validationResult)
    else validationResult.errorCollection
}