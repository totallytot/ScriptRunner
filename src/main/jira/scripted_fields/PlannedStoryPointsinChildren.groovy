package jira.scripted_fields

import com.atlassian.jira.component.ComponentAccessor;

int SP = 0;
linkManager = ComponentAccessor.getIssueLinkManager();
customFieldManager = ComponentAccessor.getCustomFieldManager();
storyPointsField = customFieldManager.getCustomFieldObject(10002L);

if (issue.getIssueType().getName() == "Epic") {

    def storiesCollection = linkManager.getOutwardLinks(issue.id);
    def storyIssue;
    if (storiesCollection!=null) {
        for (stories in storiesCollection.iterator()){
            storyIssue = stories.getDestinationObject();
        if (storyIssue.getStatus().getName().equals("Done"))
            SP += storyPointsField.getValue(storyIssue);
    }
}
}

/* FIELD DESCRIPTION
<!-- @@Formula:
import com.atlassian.jira.component.ComponentAccessor;
int SP = 0;
if (issue.get("issuetype").getName() == "Epic") {
    storiesCollection = ComponentAccessor.getIssueLinkManager().getOutwardLinks(issue.id);
    if (storiesCollection!=null) {
        for (stories : storiesCollection.iterator()){
            storyIssue = stories.getDestinationObject();
            if (!storyIssue.getStatus().getName().equals("Done"))
                SP += storyIssue.get("customfield_10002");
        }
    }
}
if (SP ==0){ return 1}
else      { return SP}
-->

*/