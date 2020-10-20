package jira_cloud.post_functions.set_field_value_based_on
// condition that starts script if the field is empty
def value = issue.fields["customfield_10036"] as String
return value == null

// script
def issueEditMeta = get("/rest/api/2/issue/" + issue.key + "/editmeta").asObject(Map)
def options = issueEditMeta.body.fields.customfield_10036.allowedValues.value
def userGroups = get("/rest/api/2/user/groups?key=" + issue.fields.reporter.key).asObject(List).body.name
def commons = userGroups.intersect(options)
put("/rest/api/2/issue/" + issue.key)
        .queryString("overrideScreenSecurity", Boolean.TRUE)
        .header('Content-Type', 'application/json')
        .body([
        fields: [
                customfield_10036: [value: commons[0]]
        ]
]).asString()