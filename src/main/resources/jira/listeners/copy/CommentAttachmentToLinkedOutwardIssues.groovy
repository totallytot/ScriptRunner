package jira.listeners.copy

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
if (commentBody && type == "Task") {
        def attacher = ComponentAccessor.attachmentManager
        def linker = ComponentAccessor.issueLinkManager
        def sourceAttaches = attacher.getAttachments(issue)

        //iterate over the issues linked to the current issue through
        linker.getOutwardLinks(issue.id).each { outwardLink ->
                def destinationIssue = outwardLink.destinationObject
                //copy comment
                commentMgr.create(destinationIssue, originalAuthor, commentBody , true)
                //get attachments from linked issue
                def linkedAttaches = attacher.getAttachments(destinationIssue)
                //check if this attachment from the linked issue already exists on the current issue
                //and if not exists copy it to the linked issue
                if (sourceAttaches) sourceAttaches.each{ sourceAttach ->
                        if (!linkedAttaches.find { linkedAttach ->
                                        sourceAttach.filename == linkedAttach.filename &&
                                        sourceAttach.filesize == linkedAttach.filesize &&
                                        sourceAttach.mimetype == linkedAttach.mimetype
                        }) attacher.copyAttachment(sourceAttach, originalAuthor, destinationIssue.key)
                }
        }
}