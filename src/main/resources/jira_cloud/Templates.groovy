package jira_cloud

import kong.unirest.Unirest

static Map getIssue(issueKey) {
    Unirest.get("/rest/api/2/issue/${issueKey}").asObject(Map).body
}

static def getCustomFiledValue(issue, customfield_id) {
    def result = Unirest.get("/rest/api/2/issue/${issue.key}?fields=${customfield_id}")
            .header('Content-Type', 'application/json')
            .asObject(Map)
    result.status == 200 ? result.body.fields[customfield_id]?.value : null
}

static List getComments(issue) {
    Unirest.get("/rest/api/2/issue/${issue.key}/comment").asObject(Map).body.comments as List
}

// find field id. CustomFields should be triggered once, as there is no reason to produce additional api calls
def customFields = Unirest.get("/rest/api/2/field").asObject(List).body.findAll { (it as Map).custom } as List<Map>
def customFieldId = customFields.find { it.name == "fieldName" }?.id

static def setSelectListField(issue, customfield_id, value) {
    def optionId = get("/rest/api/2/issue/${issue.key}/editmeta")
            .header('Content-Type', 'application/json').asObject(Map)
            .body.fields[customfield_id]?.allowedValues?.find { it.value == value}?.id
    if (optionId) {
        Unirest.put("/rest/api/2/issue/${issue.key}")
                .queryString("overrideScreenSecurity", Boolean.TRUE)
                .queryString("notifyUsers", Boolean.FALSE)
                .header("Content-Type", "application/json")
                .body([
                        fields: [
                                (customfield_id):[value:"${value}"]
                        ]
                ]).asString()
    }
}

static setTextField(issue, customfield_id, value) {
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

static def getCustomFieldValue(issue, customfield_id) {
    Unirest.get("/rest/api/2/issue/${issue.key}?fields=${customfield_id}")
            .header('Content-Type', 'application/json')
            .asObject(Map).body.fields[customfield_id]
}