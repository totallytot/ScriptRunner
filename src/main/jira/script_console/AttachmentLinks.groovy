package jira.script_console

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.attachment.Attachment

def issue = ComponentAccessor.issueManager.getIssueByCurrentKey("HAUFE-893")
def baseurl = ComponentAccessor.applicationProperties.getString("jira.baseurl")

issue.attachments.collect { Attachment attachment ->
    baseurl + "/secure/attachment/" + attachment.id + "/" + attachment.filename
}