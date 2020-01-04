package jira_cloud.listeners

if (issue.fields.issuetype.name.equals("Sub-task")){
    def issueParent =  get('/rest/api/2/issue/' + issue.fields.parent.key).asObject(Map)
    sumStoryPoints(issueParent.body)
}
else{
    if (issue.fields.issuetype.name.equals("Story"))
        if (issue.fields.subtasks.size() > 0)
            sumStoryPoints(issue)
}

void sumStoryPoints(Map issueStory)  {

    def subtasks = issueStory.fields.subtasks
    Number sum = 0

    subtasks.each{
        def subtaskIssue =  get('/rest/api/2/issue/' + it.key).asObject(Map)
        if (subtaskIssue.body.fields.customfield_10027 != null)
            sum = sum + subtaskIssue.body.fields.customfield_10027
    }

    def result = put('/rest/api/2/issue/' + issueStory.key)
            .header('Content-Type', 'application/json')
            .body([
            fields:[
                    customfield_10027: sum
            ]
    ])
            .asString()
}