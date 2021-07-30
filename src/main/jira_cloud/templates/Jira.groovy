package jira_cloud.templates

import kong.unirest.Unirest

static Map getIssue(String issueKey) {
    Unirest.get("/rest/api/3/issue/${issueKey}")
            .header("Content-Type", "application/json")
            .asObject(Map).body
}

static List<Map> getCustomFields() {
    Unirest.get("/rest/api/3/field")
            .header("Content-Type", "application/json")
            .asObject(List)
            .body
            .findAll { (it as Map).custom } as List<Map>
}

static List<Map> getIssueTypes() {
    Unirest.get("/rest/api/3/issuetype")
            .header("Content-Type", "application/json")
            .asObject(List)
            .body as List<Map>
}

static Map getEditMetaData(String issueKey) {
    Unirest.get("/rest/api/2/issue/${issueKey}/editmeta")
            .header("Content-Type", "application/json")
            .asObject(Map).body
}

static def getCustomFiledValue(Map issue, String customfieldId) {
    def result = Unirest.get("/rest/api/3/issue/${issue.key}?fields=${customfieldId}")
            .header("Content-Type", "application/json")
            .asObject(Map)
    //noinspection GroovyConditional
    result.status == 200 ? result.body.fields[customfieldId]?.value : null
}

static List getComments(Map issue) {
    Unirest.get("/rest/api/3/issue/${issue.key}/comment")
            .header("Content-Type", "application/json")
            .asObject(Map).body.comments as List
}

static def setSelectListField(Map issue, String customfieldId, String value) {
    def optionId = Unirest.get("/rest/api/3/issue/${issue.key}/editmeta")
            .header("Content-Type", "application/json").asObject(Map)
            .body.fields[customfieldId]?.allowedValues?.find { it.value == value}?.id
    if (optionId) {
        Unirest.put("/rest/api/3/issue/${issue.key}")
                .queryString("overrideScreenSecurity", Boolean.TRUE)
                .queryString("notifyUsers", Boolean.FALSE)
                .header("Content-Type", "application/json")
                .body([
                        fields: [
                                (customfieldId):[value: value]
                        ]
                ]).asString()
    }
}

static setField(String issueKey, String customfieldId, String value) {
    Unirest.put("/rest/api/3/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: [(customfieldId): value]]).asString()
}

static int setFields(String issueKey, Map fieldsValsMapping) {
    def result = Unirest.put("/rest/api/3/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: fieldsValsMapping]).asString()
    return result.status
}

static setDueDate(Map issue, String date) {
    //yyyy-MM-dd
    Unirest.put("/rest/api/3/issue/${issue.key}")
            .header("Content-Type", "application/json")
            .queryString("notifyUsers", Boolean.FALSE)
            .body([
                    fields:[
                            duedate: date
                    ]
            ]).asString()
}

static setOriginalEstimate(String issueKey, int minutes) {
    Unirest.put("/rest/api/3/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: [timetracking: [originalEstimate: "${minutes}m"]]])
            .asString()
}

static addComment(Map issue, String commentBody) {
    Unirest.post("/rest/api/3/issue/${issue.key}/comment")
            .header("Content-Type", "application/json")
            .body([
                    body: commentBody
            ]).asObject(Map)
}

static int addOptions(String customFieldId, List<String> options) {
    def customFieldIdReplaced = customFieldId.replace("customfield_", "")
    def optionsForRest = options.findResults { ["value": it.toString().trim()] }
    def result = Unirest.post("/rest/api/3/customField/${customFieldIdReplaced}/option")
            .header("Content-Type", "application/json")
            .body(["options": optionsForRest]).asString()
    return result.status
}

static List executeSearch(String jqlQuery, int startAt, int maxResults) {
    def searchRequest = Unirest.get("/rest/api/3/search")
            .queryString("jql", jqlQuery)
            .queryString("startAt", startAt)
            .queryString("maxResults", maxResults)
            .asObject(Map)
    //noinspection GroovyConditional
    searchRequest.status == 200 ? searchRequest.body.issues as List<Map> : []
}