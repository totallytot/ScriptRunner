package jira_cloud.listeners.set_field_value_based_on

import kong.unirest.Unirest
import java.text.SimpleDateFormat

def issue = getIssue("QMSNC-1") // for testing in console only

logger.info("Working with ${issue.key}")
def customFields = Unirest.get("/rest/api/2/field").asObject(List).body.findAll { (it as Map).custom } as List<Map>
def customFieldId = customFields.find { it.name == "Latest Comment" }?.id
def comments = getComments(issue)

if (!comments.isEmpty()) {
    def lastComment = comments.last()
    // formatting
    def formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
    def created = formatter.parse(lastComment.created)
    formatter = new SimpleDateFormat("dd/MM/yyyy")
    created = formatter.format(created)
    def lastCommentFormatted = "By ${lastComment.author.displayName}, ${created}:\n${lastComment.body}"

    updateTextField(issue, customFieldId, lastCommentFormatted)
}

static def getIssue(issueKey) {
    Unirest.get("/rest/api/2/issue/${issueKey}").asObject(Map).body
}

static List getComments(issue) {
    Unirest.get("/rest/api/2/issue/${issue.key}/comment").asObject(Map).body.comments as List
}

static updateTextField(issue, customfield_id, value) {
    Unirest.put("/rest/api/2/issue/${issue.key}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.TRUE)
            .header("Content-Type", "application/json")
            .body([
                    fields:[
                            (customfield_id):value
                    ]
            ]).asString()
}