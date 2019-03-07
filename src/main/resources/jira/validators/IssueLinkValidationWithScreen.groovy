package jira.validators

import org.apache.log4j.Logger
import org.apache.log4j.Level
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.IssueLinksSystemField
import webwork.action.ActionContext

def log = Logger.getLogger("check-me")
log.setLevel(Level.DEBUG)


if (issue.parentObject.issueType.name == "Product Requirement") {
    def inwardValidation = ComponentAccessor.issueLinkManager.getInwardLinks(issue.id).any {
        it.issueLinkType.inward == "has the bug" &&
                it.sourceObject.issueType.name in ["Technical Story", "User Story", "Bug (Dev)",
                                                   "Defect (Production | V&V)", "Software Implementation Bug"]
    }
    def outwardValidation = ComponentAccessor.issueLinkManager.getOutwardLinks(issue.id).any {
        it.issueLinkType.outward == "is a requirement for" &&
                it.destinationObject.issueType.name in ["Technical Story", "User Story", "Bug (Dev)",
                                                        "Defect (Production | V&V)", "Software Implementation Bug"]
    }
    def fieldManager = ComponentAccessor.getFieldManager()
    def linksSystemField = fieldManager.getField("issuelinks") as IssueLinksSystemField
    def request = ActionContext.getRequest()
    def screenValidation = false
    if (request) {
        def params = request.getParameterMap()
        def issueLinkingValue = linksSystemField.getRelevantParams(params) as IssueLinksSystemField.IssueLinkingValue
        screenValidation = ((issueLinkingValue.linkDescription == "has the bug" ||
                issueLinkingValue.linkDescription == "is a requirement for") && issueLinkingValue.linkedIssues.size() > 0)
    }
    inwardValidation || outwardValidation || screenValidation
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