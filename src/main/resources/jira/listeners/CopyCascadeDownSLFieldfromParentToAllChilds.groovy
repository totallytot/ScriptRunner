package jira.listeners

import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder

def featureIssue = event.issue
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def linkManager = ComponentAccessor.getIssueLinkManager()
def roadmapQuaterField = customFieldManager.getCustomFieldObject(18903L);

if (featureIssue.getIssueType().name == "Roadmap Feature") {

    def epicCollection = linkManager.getOutwardLinks(featureIssue.id)
    def epicIssue

    epicCollection.each {

        epicIssue = it.getDestinationObject()
        //modify epic
        def roadmapQuaterFieldValue = roadmapQuaterField.getValue(featureIssue)
        def modifiedValue = new ModifiedValue(null, roadmapQuaterFieldValue);
        roadmapQuaterField.updateValue(null, epicIssue, modifiedValue, new DefaultIssueChangeHolder())
        //looking for stories
        def storiesCollection = linkManager.getOutwardLinks(epicIssue.id)
        def storyIssue

        storiesCollection.each{
            //modify story
            storyIssue= it.getDestinationObject()
            roadmapQuaterFieldValue = roadmapQuaterField.getValue(epicIssue)
            modifiedValue = new ModifiedValue(null, roadmapQuaterFieldValue);
            roadmapQuaterField.updateValue(null, storyIssue, modifiedValue, new DefaultIssueChangeHolder())
        }
    }
}