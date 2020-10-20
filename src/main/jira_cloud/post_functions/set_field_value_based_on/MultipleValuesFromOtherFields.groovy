package jira_cloud.post_functions.set_field_value_based_on
// get list of fields and find required values
def customFields = get("/rest/api/2/field").asObject(List).body.findAll { (it as Map).custom } as List<Map>

// get field id and value for confition
def classificationId = customFields.find { it.name == "Classification" }?.id
def classificationValue = getCustomFiledValue(issue, customFields.find { it.name == "Classification" }?.id)

//logic
if (!classificationValue) {
    def complexityValue = getCustomFiledValue(issue, customFields.find { it.name == "Complexity" }?.id)
    def scopeValue = getCustomFiledValue(issue, customFields.find { it.name == "Scope" }?.id)
    def rrValue = getCustomFiledValue(issue, customFields.find { it.name == "Reporting or recall" }?.id)
    if (complexityValue == "Multiple components out of control" ||
            scopeValue.any{ it in ["Multiple product, process or document","Suspended release","Multiple sites"]} ||
            rrValue == "Required") updateSelectList(issue, classificationId, "SNC")
}

// functions
def getCustomFiledValue(issue, customfield_id) {
    def result = get("/rest/api/2/issue/${issue.key}?fields=${customfield_id}")
            .header('Content-Type', 'application/json')
            .asObject(Map)
    result.status == 200 ? result.body.fields[customfield_id]?.value : null
}

def updateSelectList(issue, customfield_id, value) {
    def optionId = get("/rest/api/2/issue/${issue.key}/editmeta")
            .header('Content-Type', 'application/json').asObject(Map).body.fields[customfield_id]?.allowedValues?.find { it.value == value}?.id
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