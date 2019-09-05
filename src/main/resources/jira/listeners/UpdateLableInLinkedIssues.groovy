package jira.listeners

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.label.LabelManager
import com.atlassian.jira.issue.link.IssueLink
import org.ofbiz.core.entity.GenericValue

def issueUpdateEvent = event as IssueEvent
def issue = issueUpdateEvent.issue
if (issue.issueType.name == "Story") {
    GenericValue change = issueUpdateEvent?.changeLog?.getRelated("ChildChangeItem")?.find { genericValue ->
        genericValue.get("field") == "labels"
    }
    if (change) {
        def oldLabels = change.get("oldstring").toString().split()
        def newLabels = change.get("newstring").toString().split()
        def labelsForLinkedIssues = newLabels.findAll { !(it in oldLabels) }
        if (labelsForLinkedIssues) {
            def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser
            def labelManager = ComponentAccessor.getComponent(LabelManager.class)
            ComponentAccessor.issueLinkManager.getOutwardLinks(issue.id).each { IssueLink issueLink ->
                labelsForLinkedIssues.each {
                    labelManager.addLabel(user, issueLink.destinationObject.id, it, false)
                }
            }
        }
    }
}