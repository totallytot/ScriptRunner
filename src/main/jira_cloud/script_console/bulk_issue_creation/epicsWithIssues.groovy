package jira_cloud.script_console.bulk_issue_creation

import kong.unirest.Unirest

int amountOfEpics = 2
int amountOfIssuesInEpic = 15
def projectKey = "TA2"
def epicSummary = "Test Summary"
def epicDescription = "Test Description"
def epicName = "Test Epic"

def customFields = customFields
def epicNameFieldId = customFields.find { it.name == "Epic Name" }?.id
def epicLinkId = customFields.find { it.name == "Epic Link" }?.id

def issueTypes = issueTypes
def epicTypeId = issueTypes.find { it.name == "Epic" }.id
def taskTypeId = issueTypes.find { it.name == "Task" }.id

def createEpicIssue = {
    Unirest.post('/rest/api/2/issue')
            .header('Content-Type', 'application/json')
            .body(
                    [
                            fields: [
                                    summary          : epicSummary,
                                    description      : epicDescription,
                                    project          : [key: projectKey],
                                    issuetype        : [id: epicTypeId],
                                    (epicNameFieldId): epicName
                            ]
                    ]).asObject(Map).body
}

def createIssueInEpic = { String epicKey ->
    Unirest.post('/rest/api/3/issue')
            .header('Content-Type', 'application/json')
            .body(
                    [
                            fields: [
                                    summary     : "Issue in Epic",
                                    project     : [key: projectKey],
                                    issuetype   : [id: taskTypeId],
                                    (epicLinkId): epicKey
                            ]
                    ]).asObject(Map).body
}

amountOfEpics.times {
    def epic = createEpicIssue()
    def epicKey = epic.key as String
    amountOfIssuesInEpic.times { createIssueInEpic(epicKey) }
}

static List<Map> getCustomFields() {
    Unirest.get("/rest/api/3/field")
            .header('Content-Type', 'application/json')
            .asObject(List)
            .body
            .findAll { (it as Map).custom } as List<Map>
}

static List<Map> getIssueTypes() {
    Unirest.get("/rest/api/3/issuetype")
            .header('Content-Type', 'application/json')
            .asObject(List)
            .body as List<Map>
}