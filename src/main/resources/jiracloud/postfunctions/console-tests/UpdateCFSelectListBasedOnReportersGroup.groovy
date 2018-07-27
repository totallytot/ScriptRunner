package jiracloud.postfunctions

def issueKey = "JPFCC-1"
def issue = get('/rest/api/2/issue/' + issueKey).asObject(Map)

def issueEditMeta = get('/rest/api/2/issue/JPFCC-1/editmeta').asObject(Map);
def options = issueEditMeta.body.fields.customfield_10036.allowedValues.value
def reporter = issue.body.fields.reporter.key

def userGroups = get('/rest/api/2/user/groups?key=' + reporter).asObject(List).body.name
def commons = userGroups.intersect(options)


def result = put("/rest/api/2/issue/" + issueKey)
        .queryString("overrideScreenSecurity", Boolean.TRUE)
        .header('Content-Type', 'application/json')
        .body([
        fields: [
                customfield_10036: [value: commons[0]]
        ]
])
        .asString()

if (result.status == 204) {
        return 'Success'
} else {
        return "${result.status}: ${result.body}"
}
//retrieve the custom field ID for the 'Scrum Team' custom field
//def fields = get('/rest/api/2/field').asObject(List).body as List<Map>
//def customFieldId = fields.find { it.name == 'Scrum Team'}.id as String //output customfield_10036
//get all fields
//def fields = get('/rest/api/2/field').asObject(List).body as List<Map>
//def customFieldValue = (issue.fields[customFieldId] as Map)?.value