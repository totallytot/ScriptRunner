package jira.listeners

import com.atlassian.applinks.api.ApplicationLink
import com.atlassian.jira.applinks.JiraApplicationLinkService
import com.atlassian.jira.bc.issue.link.RemoteIssueLinkService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.IssueEvent
import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder
import com.atlassian.jira.util.io.InputStreamConsumer
import groovy.json.JsonSlurper
import groovy.transform.Field
import org.apache.commons.io.FilenameUtils
import org.apache.http.HttpEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.lf5.util.StreamUtils

/**
 * App link is configured without OAuth without impersonation
 */

@Field final String TARGET_API_URL = "https://jira.example.com/rest/api/latest"
@Field final String TARGET_BASIC_AUTH = "Bearer "
@Field final String TARGET_PROJECT_KEY = "Test"
@Field final String TARGET_ISSUE_TYPE = "Task"
@Field final String TARGET_ISSUE_LABEL = "Label"
@Field final String SOURCE_EXECUTION_USERNAME = "service_user"
@Field final String SOURCE_APPLICATION_LINK_NAME = "Test JIRA"

def postRequest = { String url, HttpEntity entity ->
    def httpClient = HttpClientBuilder.create().build()
    def httpPost = new HttpPost(url)
    httpPost.with {
        setHeader("Authorization", TARGET_BASIC_AUTH)
        setHeader("X-Atlassian-Token", "nocheck")
        setEntity(entity)
    }
    return httpClient.execute(httpPost)
}

def scriptName = this.class.name
def logger = Logger.getLogger(scriptName)
logger.setLevel(Level.INFO)
logger.info "LISTENER START"

logger.info "INITIAL CONDITIONS CHECK"
def issueEvent = event as IssueEvent
def issue = issueEvent.issue
if (issue.issueType.name != "Report") {
    logger.info "NOT ALLOWED ISSUE TYPE - LISTENER STOPPED"
    return
}
def attachmentChange = issueEvent.changeLog?.getRelated("ChildChangeItem")?.find { it.field == "Attachment" }
def attachmentId = attachmentChange["newvalue"]
if (!attachmentChange || !attachmentId) {
    logger.info "NO ATTACHMENTS ADDED - LISTENER STOPPED"
    return
}
logger.info "INITIAL CONDITIONS PASSED"

logger.info "STARTING REMOTE ISSUE CREATION"
def jsonRemoteIssueData = """
{
    "fields": {
        "project": {
            "key": "${TARGET_PROJECT_KEY}"
        },
        "summary": "${issue.summary}",
        "issuetype": {
            "name": "${TARGET_ISSUE_TYPE}"
        },
        "description": "${issue.description}",
        "labels": [
            "${TARGET_ISSUE_LABEL}"
        ]
    }
}
"""
def issueEntity = new StringEntity(jsonRemoteIssueData, ContentType.APPLICATION_JSON)
def createIssue = postRequest("${TARGET_API_URL}/issue", issueEntity)
if (createIssue.statusLine.statusCode != 201) {
    logger.info "ERROR DURING REMOTE ISSUE CREATION - LISTENER STOPPED"
    logger.info createIssue.entity.content
    return
}
def jsonResponse = new JsonSlurper().parseText(createIssue.entity.content.text) as Map
def createdIssueKey = jsonResponse["key"]
def createdIssueId = jsonResponse["id"]
logger.info "REMOTE ISSUE CREATED: ${createdIssueKey} - ${createdIssueId}"

logger.info "STARTING SOURCE REMOTE LINK CREATION"
def sourceExecutionUser = ComponentAccessor.userManager.getUserByName(SOURCE_EXECUTION_USERNAME)
def jiraApplicationLinkService = ComponentAccessor.getComponent(JiraApplicationLinkService)
def remoteIssueLinkService = ComponentAccessor.getComponent(RemoteIssueLinkService)
def applicationLink = jiraApplicationLinkService.applicationLinks.find { ApplicationLink applicationLink ->
    applicationLink.name == SOURCE_APPLICATION_LINK_NAME
} as ApplicationLink
def globalId = "appId=${applicationLink.id}&issueId=${createdIssueId}"
def issueRemoteLink = new RemoteIssueLinkBuilder().globalId(globalId)
issueRemoteLink.with {
    issueId(issue.id)
    applicationType("com.atlassian.jira")
    applicationName(applicationLink.name)
    relationship("relates to")
    url("${applicationLink.displayUrl}/browse/${createdIssueKey}")
    summary("${issue.summary}")
    title("${createdIssueKey}")
}
def validationResult = remoteIssueLinkService.validateCreate(sourceExecutionUser, issueRemoteLink.build())
if (!validationResult.valid) {
    logger.info "SOURCE REMOTE LINK VALIDATION RESULT IS NOT VALID - LISTENER STOPPED"
    logger.info validationResult.errorCollection
    return
}
def link = remoteIssueLinkService.create(sourceExecutionUser, validationResult)
logger.info "SOURCE REMOTE LINK CREATED: ${link.remoteIssueLink.properties}"

logger.info "STARTING TARGET REMOTE LINK CREATION"
def sourceBaseUrl = ComponentAccessor.applicationProperties.getString("jira.baseurl")
def sourceJiraTitle = ComponentAccessor.applicationProperties.getString("jira.title")
def jsonRemoteLink = """
{
    "globalId": "appId=${applicationLink.id}&issueId=${issue.id}",
    "application": {                                            
        "type":"com.atlassian.jira",                      
        "name":"${sourceJiraTitle}"
    },
    "relationship":"relates to",                           
    "object": {                                            
        "url":"${sourceBaseUrl}/${issue.key}",
        "title":"${issue.key}",                             
        "summary":"${issue.summary}"
    }
}
"""
def createRemoteLink = postRequest(
        "${TARGET_API_URL}/issue/${createdIssueKey}/remotelink",
        new StringEntity(jsonRemoteLink, ContentType.APPLICATION_JSON)
)
if (createRemoteLink.statusLine.statusCode != 201) {
    logger.info "ERROR DURING TARGET REMOTE LINK CREATION (${createRemoteLink.statusLine.statusCode}) - LISTENER STOPPED"
    logger.info createRemoteLink.entity.content.text
    return
}
logger.info "TARGET REMOTE LINK CREATED: ${createRemoteLink.entity.content.text}"

logger.info "STARTING ATTACHMENT UPLOAD"
def attachmentManager = ComponentAccessor.attachmentManager
def attachment = attachmentManager.getAttachment(attachmentId as Long)
def fileName = attachment.filename
if (FilenameUtils.getBaseName(fileName).length() < 3) fileName = "__${fileName}" //java.lang.IllegalArgumentException: Prefix string too short
def tempAttachmentFile = attachmentManager.streamAttachmentContent(attachment, new FileInputStreamConsumer(fileName))
def attachmentEntity = MultipartEntityBuilder.create()
attachmentEntity.addPart("file", new FileBody(new File(tempAttachmentFile as String)))
def attachFile = postRequest("${TARGET_API_URL}/issue/${createdIssueKey}/attachments", attachmentEntity.build())
if (attachFile.statusLine.statusCode != 200) {
    logger.info "ERROR DURING ATTACHMENT UPLOAD - LISTENER STOPPED"
    logger.info attachFile.entity.content.text
    return
}
logger.info "ATTACHMENT UPLOADED: ${attachFile.entity.content.text}"
logger.info "LISTENER END"

class FileInputStreamConsumer implements InputStreamConsumer {
    private final String fileName

    FileInputStreamConsumer(String fileName) {
        this.fileName = fileName
    }

    @Override
    File withInputStream(InputStream inputStream) throws IOException {
        final File file = File.createTempFile("${FilenameUtils.getBaseName(fileName)}-", ".${FilenameUtils.getExtension(fileName)}")
        StreamUtils.copy(inputStream, new FileOutputStream(file))
        return file
    }
}