package jira_cloud

import kong.unirest.Unirest

static Map getIssue(String issueKey) {
    Unirest.get("/rest/api/2/issue/${issueKey}")
            .header('Content-Type', 'application/json')
            .asObject(Map).body
}

static List<Map> getCustomFields() {
    Unirest.get("/rest/api/2/field")
            .header('Content-Type', 'application/json')
            .asObject(List)
            .body
            .findAll { (it as Map).custom } as List<Map>
}

static Map getEditMetaData(String issueKey) {
    Unirest.get("/rest/api/2/issue/${issueKey}/editmeta")
            .header('Content-Type', 'application/json')
            .asObject(Map).body
}

static def getCustomFiledValue(issue, customfield_id) {
    def result = Unirest.get("/rest/api/2/issue/${issue.key}?fields=${customfield_id}")
            .header('Content-Type', 'application/json')
            .asObject(Map)
    result.status == 200 ? result.body.fields[customfield_id]?.value : null
}

static List getComments(issue) {
    Unirest.get("/rest/api/2/issue/${issue.key}/comment")
            .header('Content-Type', 'application/json')
            .asObject(Map).body.comments as List
}

static def setSelectListField(issue, customfield_id, value) {
    def optionId = Unirest.get("/rest/api/2/issue/${issue.key}/editmeta")
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

static setField(String issueKey, String customfield_id, value) {
    Unirest.put("/rest/api/2/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: [(customfield_id): value]]).asString()
}

static int setFields(String issueKey, Map fieldsValsMapping) {
    def result = Unirest.put("/rest/api/2/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: fieldsValsMapping]).asString()
    return result.status
}

static setDueDate(issue, String date) {
    //yyyy-MM-dd
    Unirest.put("/rest/api/2/issue/${issue.key}")
            .header('Content-Type', 'application/json')
            .queryString("notifyUsers", Boolean.FALSE)
            .body([fields:[duedate:date]]).asString()

}

static setOriginalEstimate(issueKey, minutes) {
    Unirest.put("/rest/api/2/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: [timetracking: [originalEstimate: "${minutes}m"]]])
            .asString()
}

static addComment(issue, commentBody) {
    Unirest.post("/rest/api/2/issue/${issue.key}/comment")
            .header('Content-Type', 'application/json')
            .body([body:commentBody]).asObject(Map)
}

static int addOptions(String customFieldId, List<String> options) {
    customFieldId = customFieldId.replace("customfield_", "")
    options = options.findResults { ["value": it.toString().trim()] }
    def result = Unirest.post("/rest/api/3/customField/${customFieldId}/option")
            .header("Content-Type", "application/json")
            .body(["options": options]).asString()
    return result.status
}

static List executeSearch(String jqlQuery, int startAt, int maxResults) {
    def searchRequest = Unirest.get("/rest/api/2/search")
            .queryString("jql", jqlQuery)
            .queryString("startAt", startAt)
            .queryString("maxResults", maxResults)
            .asObject(Map)
    searchRequest.status == 200 ? searchRequest.body.issues as List : null
}