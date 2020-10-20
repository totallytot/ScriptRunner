package jira_cloud.listeners

import kong.unirest.Unirest

logger.info("Working with ${issue.key}")
assert issue.fields.issuetype.name.toString() in ["Action", "Effectiveness check"]

def dueDateValue = issue.fields.duedate as String
assert dueDateValue != null
def parentDueDateValue = Unirest.get("/rest/api/2/issue/${issue.fields.parent.key}").asObject(Map).body.fields.duedate as String
assert parentDueDateValue != null

def dateFormat = "yyyy-MM-dd"
def childDueDate = new Date().parse(dateFormat, dueDateValue)
def parentDueDate = new Date().parse(dateFormat, parentDueDateValue)

if (childDueDate.after(parentDueDate)) {
    setDueDate(issue, parentDueDateValue)
    def commentBody = "Due Date was updated automatically because the due date of the sub-task cannot be later then it's parent duedate"
    addComment(issue, commentBody)
}

static addComment(issue, commentBody) {
    Unirest.post("/rest/api/2/issue/${issue.key}/comment")
            .header('Content-Type', 'application/json')
            .body([body:commentBody]).asObject(Map)
}

static setDueDate(issue, String date) {
    //yyyy-MM-dd
    Unirest.put("/rest/api/2/issue/${issue.key}")
            .header('Content-Type', 'application/json')
            .queryString("notifyUsers", Boolean.FALSE)
            .body([fields:[duedate:date]]).asString()
}