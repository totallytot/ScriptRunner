package jira_cloud.script_console.bulk_issue_creation

import kong.unirest.Unirest

int amountOfIssues = 8
def projectKey = "TA2"
def epicSummary = "Test Summary"
def epicDescription = "Test Description"
def epicName = "Test Epic"
def epicNameField = "Epic Name"
def customFields = customFields
def epicNameFieldId = customFields.find { it.name == epicNameField }?.id
def epicType = Unirest.get("/rest/api/2/issuetype").asObject(List).body.find { it["name"] == "Epic" }["id"]

amountOfIssues.times {
    Unirest.post('/rest/api/2/issue')
            .header('Content-Type', 'application/json')
            .body(
                    [
                            fields: [
                                    summary          : epicSummary,
                                    description      : epicDescription,
                                    project          : [key: projectKey],
                                    issuetype        : [id: epicType],
                                    (epicNameFieldId): epicName
                            ]
                    ]).asString().body
}

static List<Map> getCustomFields() {
    Unirest.get("/rest/api/2/field")
            .header('Content-Type', 'application/json')
            .asObject(List)
            .body
            .findAll { (it as Map).custom } as List<Map>
}