package jira_cloud

String getCustomFiledValue(String issueKey, String customFieldNameID) {
    def result = get("/rest/api/2/issue/${issueKey}?fields=${customFieldNameID}")
            .header('Content-Type', 'application/json')
            .asObject(Map)
    if (result.status == 200) {
        return result.body.fields[customFieldName].value
    } else {
        return null
    }
}