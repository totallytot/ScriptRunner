package jiracloud

def issuek = get('/rest/api/2/issue/' + issue.key).asObject(Map)
if (issuek.body.fields.issuetype.name.equals("Sub-task")){
    sumStoryPoints(issuek.body.fields.parent.key)
}
else{
    if (issuek.body.fields.issuetype.name.equals("Story"))
        sumStoryPoints(issuek.body.key)
}

void sumStoryPoints(String issueKey)  {
    def issue =  get('/rest/api/2/issue/' + issueKey).asObject(Map)
    def subtasks = issue.body.fields.subtasks
    Number sum = 0

    subtasks.each{
        def subtaskIssue =  get('/rest/api/2/issue/' + it.key).asObject(Map)
        if (subtaskIssue.body.fields.customfield_10027 != null)
            sum = sum + subtaskIssue.body.fields.customfield_10027
    }

    def result = put('/rest/api/2/issue/' + issueKey)
            .header('Content-Type', 'application/json')
            .body([
            fields:[
                    customfield_10027: sum
            ]
    ])
            .asString()
}