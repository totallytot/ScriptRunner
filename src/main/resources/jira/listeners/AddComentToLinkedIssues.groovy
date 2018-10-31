package jira.listeners

import com.atlassian.jira.component.ComponentAccessor

def linker = ComponentAccessor.issueLinkManager
def commentMgr = ComponentAccessor.commentManager
def issue = event.issue

// gather the original author and comment body from the original issues comment
def type = issue.issueType.name
def newComment = event.comment
def originalAuthor = newComment.authorApplicationUser
def commentBody = newComment.body

// get original issue's linked issues and create comment on the linked issue
if (commentBody != null && type == "Service Request") {
    linker.getOutwardLinks(issue.id).each {
        commentMgr.create(it.destinationObject, originalAuthor, commentBody , true)
    }
}