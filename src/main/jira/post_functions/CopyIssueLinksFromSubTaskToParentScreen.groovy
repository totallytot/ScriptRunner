package jira.post_functions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.IssueLinksSystemField
import com.atlassian.jira.issue.link.IssueLinkTypeManager
import webwork.action.ActionContext

def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def linkManager = ComponentAccessor.issueLinkManager

def childInwardLinks = linkManager.getInwardLinks(issue.id).findAll {it.issueLinkType.name != "jira_subtask_link"}
if (childInwardLinks) {
    childInwardLinks.each {
        linkManager.createIssueLink(it.sourceObject.id, issue.parentObject.id, it.issueLinkType.id, it.sequence, currentUser)
    }
}
def childOutwardsLinks = linkManager.getOutwardLinks(issue.id)
if (childOutwardsLinks) {
    childOutwardsLinks.each {
        linkManager.createIssueLink(issue.parentObject.id, it.destinationObject.id, it.issueLinkType.id, it.sequence, currentUser)
    }
}
//screen
def linksSystemField = ComponentAccessor.fieldManager.getField("issuelinks") as IssueLinksSystemField
def request = ActionContext.request
if (request) {
    def params = request.parameterMap
    def issueLinkingValue = linksSystemField.getRelevantParams(params) as IssueLinksSystemField.IssueLinkingValue
    if (issueLinkingValue.linkedIssues) {
        def issueLinkTypeManager = ComponentAccessor.getComponentOfType(IssueLinkTypeManager.class)
        def linkedIssuesObjects = issueLinkingValue.linkedIssues.collect {
            ComponentAccessor.issueManager.getIssueObject(it)
        }
        def isInwardLink = issueLinkTypeManager.issueLinkTypes.any { it.inward == issueLinkingValue.linkDescription }
        if (isInwardLink) {
            def issueLinkType = issueLinkTypeManager.issueLinkTypes.find {
                it.inward == issueLinkingValue.linkDescription
            }
            linkedIssuesObjects.each {
                linkManager.createIssueLink(it.id, issue.parentObject.id, issueLinkType.id, 0, currentUser)
            }
        } else {
            def issueLinkType = issueLinkTypeManager.issueLinkTypes.find {
                it.outward == issueLinkingValue.linkDescription
            }
            linkedIssuesObjects.each {
                linkManager.createIssueLink(issue.parentObject.id, it.id, issueLinkType.id, 0, currentUser)
            }
        }
    }
}