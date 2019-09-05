package jira.listeners

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.label.LabelManager
import com.atlassian.jira.issue.link.IssueLink
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.ofbiz.core.entity.GenericValue

def log = Logger.getLogger("check-me-dude")
log.setLevel(Level.DEBUG)
def issueUpdateEvent = event as IssueEvent
GenericValue change = issueUpdateEvent?.changeLog?.getRelated("ChildChangeItem")?.find {  genericValue ->
    genericValue.get("field") == "labels"
}
if (change) {
    def oldLabels = change.get("oldstring").toString().split()
    def newLabels = change.get("newstring").toString().split()
    def labelsForLinkedIssues = newLabels.findAll { !(it in oldLabels)}
    def issue = ComponentAccessor.issueManager.getIssueObject("PFT-54")
    if (issue.issueType.name == "Story") {
        def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser
        def labelManager = ComponentAccessor.getComponent(LabelManager.class)
        ComponentAccessor.issueLinkManager.getOutwardLinks(issue.id).each { IssueLink issueLink ->
            labelsForLinkedIssues.each {
                labelManager.addLabel(user, issueLink.destinationObject.id, it, false)
            }
        }
    }
}