package jira.validators

import org.apache.log4j.Logger
import org.apache.log4j.Level
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.IssueLinksSystemField
import webwork.action.ActionContext

def log = Logger.getLogger("check-me")
log.setLevel(Level.DEBUG)

def issueTypes = ["Technical Story", "User Story", "Bug (Dev)", "Defect (Production | V&V)", "Software Implementation Bug"]
if (issue.parentObject.issueType.name == "Product Requirement") {
    runIssueLinksValidation("has the bug", "is a requirement for", issueTypes)
} else if (issue.parentObject.issueType.name == "Software Specification") {
    runIssueLinksValidation("has the bug", "is a specification for", issueTypes)
}

boolean runIssueLinksValidation (String inwardLink, String outwardLink, List issueTypeValidation) {
    def inwardLinksValidation = ComponentAccessor.issueLinkManager.getInwardLinks(issue.id).any {
        it.issueLinkType.inward == inwardLink && it.sourceObject.issueType.name in issueTypeValidation
    }
    def outwardLinksValidation = ComponentAccessor.issueLinkManager.getOutwardLinks(issue.id).any {
        it.issueLinkType.outward == outwardLink && it.destinationObject.issueType.name in issueTypeValidation
    }
    def linksSystemField = ComponentAccessor.fieldManager.getField("issuelinks") as IssueLinksSystemField
    def request = ActionContext.request
    def screenLinksValidation = false
    if (request) {
        def params = request.parameterMap
        def issueLinkingValue = linksSystemField.getRelevantParams(params) as IssueLinksSystemField.IssueLinkingValue
        screenLinksValidation = ((issueLinkingValue.linkDescription in [inwardLink, outwardLink])
                && issueLinkingValue.linkedIssues.size() > 0
                && issueLinkingValue.linkedIssues.any {
            ComponentAccessor.issueManager.getIssueObject(it).issueType.name in issueTypeValidation
        })
    }
    inwardLinksValidation || outwardLinksValidation || screenLinksValidation
}

/*
ComponentAccessor.issueLinkManager.getInwardLinks(issue.id).each {
    log.debug(it.issueLinkType.inward)
    log.debug(it.sourceObject.issueType.name)
    log.debug(it.id)
}
log.debug("===============")
ComponentAccessor.issueLinkManager.getOutwardLinks(issue.id).each {
    log.debug(it.issueLinkType.outward)
    log.debug(it.destinationObject.issueType.name)
    log.debug(it.id)
}
*/