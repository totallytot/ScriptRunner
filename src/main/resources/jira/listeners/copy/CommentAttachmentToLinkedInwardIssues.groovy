package jira.listeners.copy

import com.atlassian.jira.component.ComponentAccessor
import org.apache.log4j.Logger
import org.apache.log4j.Level
import org.ofbiz.core.entity.GenericValue

/**
 * Events: issue updated, issue commented, generic event
 */

def logger = Logger.getLogger("test-me-pls")
logger.setLevel(Level.DEBUG)

def attachmentIds = []
def changeItems = event.changeLog?.getRelated("ChildChangeItem") as List<GenericValue>
if (changeItems) changeItems.each { GenericValue genericValue ->
    if (genericValue["field"] == "Attachment" && genericValue["newvalue"]) attachmentIds << genericValue["newvalue"]
}
logger.debug(event.changeLog?.getRelated("ChildChangeItem"))
logger.debug(attachmentIds)

def executionUser = ComponentAccessor.userManager.getUserByName("sr_automation")
def inwardIssues = ComponentAccessor.issueLinkManager.getInwardLinks(event.issue.id).collect {it.sourceObject}
if (!attachmentIds.empty && !inwardIssues.empty) {
    def attachmentManager = ComponentAccessor.attachmentManager
    def attachments = attachmentIds.collect { attachmentManager.getAttachment(it as Long) }
    inwardIssues.each { issue ->
        attachments.each { attachment -> attachmentManager.copyAttachment(attachment, executionUser, issue.key)}
    }
}

if (event.comment && !inwardIssues.empty) {
    def originalAuthor = event.comment.authorApplicationUser.name
    def originalBody = event.comment.body
    def newBody = """Author: ${originalAuthor}
    ${originalBody}
    """
    def commentManager = ComponentAccessor.commentManager
    inwardIssues.each {
        commentManager.create(it, executionUser, newBody, true)
    }
}