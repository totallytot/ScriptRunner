package jira_cloud.listeners

import kong.unirest.Unirest

def isSubTask = Boolean.parseBoolean(issue.fields.issuetype.subtask.toString())
logger.info("issue_event_type_name: ${issue_event_type_name}")
if ((issue_event_type_name == "issue_created" && isSubTask) ||
        (issue_event_type_name == "issue_updated" && isSubTask && changelog.toString().contains("field:duedate"))) {

    // populate all required vars
    def childIssue = issue
    logger.info("Child Issue: ${childIssue.key}")
    def childDueDate = childIssue.fields.duedate as String
    logger.info("Child Due Date: ${childDueDate}")
    def parentIssue = Unirest.get("/rest/api/2/issue/${issue.fields.parent.key}").asObject(Map).body
    logger.info("Parent Issue: ${parentIssue.key}")
    def parentDueDate = parentIssue.fields.duedate as String
    logger.info("Parent Due Date: ${parentDueDate}")
    def customFields = Unirest.get("/rest/api/2/field").asObject(List).body.findAll { (it as Map).custom } as List<Map>
    def stddcfId = customFields.find { it.name == "Sub Tasks Due Dates" }?.id

    // format dates for comparision, compare and if required update value and leave comment
    def dateFormat = "yyyy-MM-dd"
    def childValue = new Date().parse(dateFormat, childDueDate)
    def parentValue = new Date().parse(dateFormat, parentDueDate)
    if (childValue.after(parentValue)) {
        setDueDate(childIssue, parentDueDate)
        def commentBody = "Due Date was updated automatically because the due date of the sub-task cannot be later then it's parent duedate"
        addComment(childIssue, commentBody)
    }
    updateTextField(parentIssue, stddcfId, generateValueForStddcf(parentIssue))
}

static def getIssue(issueKey) {
     Unirest.get("/rest/api/2/issue/${issueKey}").asObject(Map).body
}

static String generateValueForStddcf(parentIssue) {
    def subtasksKeys = parentIssue.fields.subtasks.key as List
    def value = new StringBuilder()
    value.append("${parentIssue.key} ${parentIssue.fields.issuetype.name} ${parentIssue.fields.summary} ${parentIssue.fields.duedate}\n")
    subtasksKeys.each {
        def subtask = getIssue(it)
        value.append("${subtask.key} ${subtask.fields.issuetype.name} ${subtask.fields.summary} ${subtask.fields.duedate}\n")
    }
    value as String
}

static updateTextField(issue, customfield_id, value) {
    Unirest.put("/rest/api/2/issue/${issue.key}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields:[(customfield_id):value]]).asString()
}

static setDueDate(issue, String date) {
    //yyyy-MM-dd
    Unirest.put("/rest/api/2/issue/${issue.key}")
            .header('Content-Type', 'application/json')
            .queryString("notifyUsers", Boolean.FALSE)
            .body([fields:[duedate:date]]).asString()
}

static addComment(issue, commentBody) {
    Unirest.post("/rest/api/2/issue/${issue.key}/comment")
            .header('Content-Type', 'application/json')
            .body([body:commentBody]).asObject(Map)
}