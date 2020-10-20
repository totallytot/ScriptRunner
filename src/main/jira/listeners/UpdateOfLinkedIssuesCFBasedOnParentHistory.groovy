package jira.listeners

import com.atlassian.jira.component.ComponentAccessor
import java.text.SimpleDateFormat

def issue = event.issue
def changeHistoryManager = ComponentAccessor.getChangeHistoryManager()
def linkManager = ComponentAccessor.getIssueLinkManager()
int changeID = changeHistoryManager.getAllChangeItems(issue).get(changeHistoryManager.getAllChangeItems(issue).size()-1).changeGroupId
def listUpdated = changeHistoryManager.getChangeHistoryById(changeID).getChangeItemBeans()
def catchedChange = listUpdated.find{  it.field == 'Start date'  }
if (catchedChange != null){

    def childCollection = linkManager.getOutwardLinks(issue.id)
    if (childCollection!=null){

        def childIssue
        childCollection.each {
            if (it.getIssueLinkType().outward == 'SF-depends on'){

                childIssue = it.getDestinationObject()
                CopyValue(issue,childIssue,16911L)
            }
        }
    }
}

def CopyValue(def outIssue, def inIssue, Long Field){
    def updateDate = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(Field)
    def user = "tech_user"
    def applicationUser = ComponentAccessor.getUserManager().getUserByKey(user)
    def issueService = ComponentAccessor.getIssueService()
    def issueInputParameters = issueService.newIssueInputParameters()
    def dateFormat = new SimpleDateFormat("dd/MMM/YY")
    Calendar createdDate = Calendar.getInstance()
    createdDate.setTime(updateDate.getValue(outIssue))
    def startDate = dateFormat.format(updateDate.getValue(outIssue))
    issueInputParameters.addCustomFieldValue(updateDate.getIdAsLong(),startDate )
    def validationResult = issueService.validateUpdate(applicationUser, inIssue.getId(), issueInputParameters)
    if (validationResult.isValid()) {
        issueService.update(applicationUser, validationResult).hasWarnings()
    }
}
