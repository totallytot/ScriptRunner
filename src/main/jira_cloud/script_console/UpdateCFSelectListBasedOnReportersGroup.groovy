package jira_cloud.script_console

def issueKey = "JPFCC-1"
def issue = get('/rest/api/2/issue/' + issueKey).asObject(Map).body

if (getCustomFiledValue(issueKey, "customfield_10036") == null)
{
    def issueEditMeta = get("/rest/api/2/issue/" + issueKey + "/editmeta").asObject(Map)
    def options = issueEditMeta.body.fields.customfield_10036.allowedValues.value
    def reporter = issue.fields.reporter.key
    def userGroups = get('/rest/api/2/user/groups?key=' + reporter).asObject(List).body.name
    def commons = userGroups.intersect(options)
    def result = put("/rest/api/2/issue/" + issueKey)
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .header('Content-Type', 'application/json')
            .body([
            fields: [
                    customfield_10036: [value: commons[0]]
            ]
    ]).asString()

    if (result.status == 204) {
        return 'Success'
    } else {
        return "${result.status}: ${result.body}"
    }
}

String getCustomFiledValue(String issueKey, String customFieldName) {
    def result = get("/rest/api/2/issue/" + issueKey + "?fields=${customFieldName}")
            .header('Content-Type', 'application/json')
            .asObject(Map)
    if (result.status == 200) {
        try {
            return result.body.fields[customFieldName].value
        } catch (NullPointerException e) {
            return null
        }
    } else {
        return null
    }
}
//retrieve the custom field ID for the 'Scrum Team' custom field
//def fields = get('/rest/api/2/field').asObject(List).body as List<Map>
//def customFieldId = fields.find { it.name == 'Scrum Team'}.id as String //output customfield_10036
//get all fields
//def fields = get('/rest/api/2/field').asObject(List).body as List<Map>
//def customFieldValue = (issue.fields[customFieldId] as Map)?.value