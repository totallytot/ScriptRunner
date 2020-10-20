package jira.listeners.sync

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.user.ApplicationUser
import org.ofbiz.core.entity.GenericValue
import org.apache.log4j.Logger
import org.apache.log4j.Level

def issue = event.issue as MutableIssue
def affectedLinkName = "PM/Defect link"
def executionUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def changeItems = event.changeLog.getRelated("ChildChangeItem")
def issueLinkManager = ComponentAccessor.issueLinkManager
def log = Logger.getLogger("check-me-please")
log.setLevel(Level.DEBUG)

// condition
def allowedIssueTypes = ["Problem Report", "Defect", "Change Request"]
if (!allowedIssueTypes.contains(issue.issueType.name)) return
def wasAffectsVersionChanged = changeItems.any { it.get("field").toString().equalsIgnoreCase("version") }
if (!wasAffectsVersionChanged) return
def hasPmDefectLink = issueLinkManager.getLinkCollection(issue, executionUser).linkTypes.any {
    it.name == affectedLinkName
}
if (!hasPmDefectLink) return
log.debug "Working with ${issue.key}"
log.debug "Version change items ${changeItems.findAll { it.get("field").toString().equalsIgnoreCase("version") }}"
// find linked issue if inward and outward references have the same name
def inwardIssues = issueLinkManager.getInwardLinks(issue.id).findResults {
    it.issueLinkType.name == "PM/Defect link" && it.destinationObject.key == issue.key ? it.sourceObject : null
}
def outwardIssues = issueLinkManager.getOutwardLinks(issue.id).findResults {
    it.issueLinkType.name == "PM/Defect link" && it.sourceObject.key == issue.key ? it.destinationObject : null
}
def affectedIssues = inwardIssues + outwardIssues
if (!affectedIssues) return
log.debug "Condition passed"

List<GenericValue> versionChangeItemsForAdding = changeItems.findAll {
    it.get("field").toString().equalsIgnoreCase("version") && it.get("newstring")
}
log.debug "versionChangeItemsForAdding: ${versionChangeItemsForAdding}"
List<GenericValue> versionChangeItemsForRemoval = changeItems.findAll {
    it.get("field").toString().equalsIgnoreCase("version") && it.get("oldstring")
}
log.debug "versionChangeItemsForRemoval: ${versionChangeItemsForRemoval}"
def versionNameIdMappingForAdding = versionChangeItemsForAdding.collectEntries { GenericValue genericValue ->
    [(genericValue.get("newstring")): genericValue.get("newvalue")]
}
log.debug "versionNameIdMappingForAdding: ${versionNameIdMappingForAdding}"
def versionNameIdMappingForRemoval = versionChangeItemsForRemoval.collectEntries { GenericValue genericValue ->
    [(genericValue.get("oldstring")): genericValue.get("oldvalue")]
}
log.debug "versionNameIdMappingForRemoval: ${versionNameIdMappingForRemoval}"
def versionIdsForAdding = versionNameIdMappingForAdding.values().collect { Long.parseLong(it.toString()) }
log.debug "versionIdsForAdding: ${versionIdsForAdding}"
def versionIdsForRemoval = versionNameIdMappingForRemoval.values().collect { Long.parseLong(it.toString()) }
log.debug "versionIdsForRemoval ${versionIdsForRemoval}"

affectedIssues.each {
    def versionIdsForUpdate = it.affectedVersions.collect { version -> version.id }
    versionIdsForUpdate += versionIdsForAdding
    if (!versionIdsForUpdate.empty) versionIdsForUpdate -= versionIdsForRemoval
    setAffectedVersions(executionUser, it as MutableIssue, versionIdsForUpdate as Long[])
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