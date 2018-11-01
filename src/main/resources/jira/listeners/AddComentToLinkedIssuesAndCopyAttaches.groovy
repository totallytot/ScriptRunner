package jira.listeners

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue

def commentMgr = ComponentAccessor.commentManager
def issue = event.issue as MutableIssue

// gather the original author and comment body from the original issues comment
def type = issue.issueType.name
def newComment = event.comment
def originalAuthor = newComment.authorApplicationUser
def commentBody = newComment.body

// get original issue's linked issues and create comment on the linked issue
if (commentBody && type == "Service Request") {
        def attacher = ComponentAccessor.attachmentManager
        def linker = ComponentAccessor.issueLinkManager
        linker.getOutwardLinks(issue.id).each {
        commentMgr.create(it.destinationObject, originalAuthor, commentBody , true)
        if (attacher.getAttachments(issue)) attacher.copyAttachments(issue, originalAuthor, it.destinationObject.key)
    }
}