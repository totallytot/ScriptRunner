package jira_cloud.post_functions

def customFields = get("/rest/api/2/field")
        .asObject(List)
        .body
        .findAll { (it as Map).custom } as List<Map>

def toUpdate1CfId = customFields.find { it.name == 'Custom Field 1' }?.id
def toUpdate2CfId = customFields.find { it.name == 'Custom Field 2' }?.id
def toUpdate3CfId = customFields.find { it.name == 'Custom Field 3' }?.id

def tomorrowStr = (new Date() + 1).format("yyyy-MM-dd'T'HH:mm:ssZ", TimeZone.getTimeZone("UTC")) // date format in iso8601
def resp = put("/rest/api/2/issue/\${issue.key}")
        .header('Content-Type', 'application/json')
        .body([
        fields: [
                fixVersions      : ['1.1'],
                component        : ['MyComponent'],
                description      : 'A generated description',
                (toUpdate1CfId): 'Some text value',
                (toUpdate2CfId): tomorrowStr,
                (toUpdate3CfId): 'admin'
        ]
])
        .asString()
assert resp.status == 204

/*
curl --user email@example.com:<api_token> \
  --header 'Accept: application/json' \
  --url 'https://your-domain.atlassian.net/rest/api/2/user/groups'
        {
            "name": "jira-administrators",
            "self": "http://www.example.com/jira/rest/api/2/group?groupname=jira-administrators"
        }
*/

