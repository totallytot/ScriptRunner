package jira_cloud.listeners.set_field_value_based_on

import kong.unirest.Unirest

def sourceIssueResponse = Unirest.get("/rest/api/2/issue/${issueLink.sourceIssueId}").asObject(Map)
assert sourceIssueResponse.status == 200
def issue = sourceIssueResponse.body

if (issueLink.issueLinkType.name.toString() == "jira_subtask_link" && issue.fields.issuetype.subtask.toString() == "false") {
    logger.info("Working with ${issue.key}")
    def subtasks = issue.fields.subtasks.key as List
    if (!subtasks.isEmpty()) {
        def value = new StringBuilder()
        value.append("${issue.key} ${-> issue.fields.issuetype.name} ${-> issue.fields.duedate}\n")
        subtasks.each {
            def subtask = getIssue(it)
            value.append("${subtask.key} ${-> subtask.fields.issuetype.name} ${-> subtask.fields.duedate}\n")
        }
        def customFields = Unirest.get("/rest/api/2/field").asObject(List).body.findAll { (it as Map).custom } as List<Map>
        def customFieldId = customFields.find { it.name == "Sub Tasks Due Dates" }?.id
        assert customFieldId != null
        updateTextField(issue, customFieldId, value)
    }
}

static Map getIssue(issueKey) {
    Unirest.get("/rest/api/2/issue/${issueKey}").asObject(Map).body
}

static updateTextField(issue, customfield_id, value) {
    Unirest.put("/rest/api/2/issue/${issue.key}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.TRUE)
            .header("Content-Type", "application/json")
            .body([fields:[(customfield_id):value]]).asString()
}