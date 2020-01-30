package jira_cloud

def getIssue(issueKey) {
    get("/rest/api/2/issue/${issueKey}").asObject(Map).body
}

def getCustomFiledValue(issue, customfield_id) {
    def result = get("/rest/api/2/issue/${issue.key}?fields=${customfield_id}")
            .header('Content-Type', 'application/json')
            .asObject(Map)
    result.status == 200 ? result.body.fields[customfield_id]?.value : null
}

// find field id. CustomFields should be triggered once, as there is no reason to produce additional api calls
def customFields = get("/rest/api/2/field").asObject(List).body.findAll { (it as Map).custom } as List<Map>
def getCustomFieldID = customFields.find { it.name == "fieldName" }?.id

// update select list field
def updateSelectList(issue, customfield_id, value) {
    def optionId = get("/rest/api/2/issue/${issue.key}/editmeta")
            .header('Content-Type', 'application/json').asObject(Map)
            .body.fields[customfield_id]?.allowedValues?.find { it.value == value}?.id
    if (optionId) {
        put("/rest/api/2/issue/${issue.key}")
                .queryString("overrideScreenSecurity", Boolean.TRUE)
                .header("Content-Type", "application/json")
                .body([
                        fields: [
                                (customfield_id):[value:"${value}"]
                        ]
                ]).asString()
    }
}
