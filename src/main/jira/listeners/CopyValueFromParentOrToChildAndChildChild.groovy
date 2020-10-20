package jira.listeners

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption

def issue = event.issue
def linkManager = ComponentAccessor.getIssueLinkManager()

if (issue.getIssueType().name == "Roadmap Feature") {

    def epicCollection = linkManager.getOutwardLinks(issue.id)
    def epicIssue

    if (epicCollection!=null){

        epicCollection.each {

            epicIssue = it.getDestinationObject()
            CopyValue(issue,epicIssue,18903L,18903L)
            def storiesCollection = linkManager.getOutwardLinks(epicIssue.id)
            def storyIssue

            if (storiesCollection!=null){

                storiesCollection.each{

                    storyIssue= it.getDestinationObject()
                    CopyValue(epicIssue,storyIssue,18903L,18903L)

                }
            }
        }

    }
}

else{

    def storyCollection = linkManager.getInwardLinks(issue.id)   //get parent

    if (storyCollection != null) {

        def parentIssue = storyCollection[0].getSourceObject()
        CopyValue(parentIssue,issue,18903L,18903L)

    }
}




def CopyValue(Issue outIssue, Issue inIssue, Long outField, Long inField){


    def optionsManager = ComponentAccessor.getOptionsManager()

    String user = "tech_user";
    def applicationUser = ComponentAccessor.getUserManager().getUserByKey(user)
    def issueService  = ComponentAccessor.getIssueService()
    def issueInputParam = issueService.newIssueInputParameters()
    def customFieldManager = ComponentAccessor.getCustomFieldManager()
    def outFieldObj = customFieldManager.getCustomFieldObject(outField)
    def inFieldObj = customFieldManager.getCustomFieldObject(inField)
    List<LazyLoadedOption> outFieldValue = (List<LazyLoadedOption>) outIssue.getCustomFieldValue(outFieldObj)

    String[] val = new String[outFieldValue.size()];

    for (int i = 0; i < outFieldValue.size(); i++) {
        val[i] = outFieldValue.get(i).getOptionId().toString();
    }

    issueInputParam.addCustomFieldValue(inField, val)
    def validatedResult = issueService.validateUpdate(applicationUser, inIssue.getId(), issueInputParam)

    if (validatedResult.isValid()){

        def result  =  issueService.update(applicationUser, validatedResult)
    }
}

