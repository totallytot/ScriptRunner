package jira.post_functions.transitions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.workflow.TransitionOptions.Builder
import org.apache.log4j.Logger
import org.apache.log4j.Level

def log = Logger.getLogger("check")
log.setLevel(Level.DEBUG)
def checkStatus = 0
def task
def issueLinkManager = ComponentAccessor.getIssueLinkManager()

task=issue
if (issue.isSubTask()){
    log.debug "issue $issue is subtask"
    task= issue.getParentObject()
    def allsubtasks = task.getSubTaskObjects()
    allsubtasks.each{ subik->
        if (subik.getStatus().name.equals('Blocked')||subik.getStatus().name.equals('Stopped')) checkStatus++
    }
    if (checkStatus==1) {
        transitIssue(281,task)
        log.debug "issue  $task sent"
    }
    else {
        log.debug "subik $issue not first"
        return
    }
}
else{   if (task.getProjectObject().name!='Master (HOF)' && task.getProjectObject().name!='EpicsWithoutIssues (HOF)'){
    def epic = task.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName('Epic Link'))
    def linksOfEpic = issueLinkManager.getOutwardLinks(epic.getId())
    def taskinepic
    linksOfEpic.each{ issueInEpic->
        taskinepic = issueInEpic.getDestinationObject()
        if (taskinepic.getStatus().name.equals('Blocked')||taskinepic.getStatus().name.equals('Stopped')) checkStatus++
    }
    if (checkStatus==1) {
        transitIssue(281,epic)
        log.debug "$epic epic sent"
        def inlink = issueLinkManager.getInwardLinks(epic.getId()).find {link-> link.getIssueLinkType().getId()==10504}
        if (inlink!=null) {
            transitIssue(281,inlink.getSourceObject())
            log.debug "$inlink.getSourceObject() feature sent"
        }
    }
    else log.debug "task $task not first"
}
else log.debug "$task is feature or epic"
}
def transitIssue (int transitionId, Issue parentIssue){
    def applicationUser = ComponentAccessor.getUserManager().getUserByKey("tech_user")
    def issueService = ComponentAccessor.getIssueService();
    def issueInputParameters = issueService.newIssueInputParameters();
    def builder =  new Builder()
    def transopt = builder.skipConditions().skipValidators().skipPermissions()
    def transitionValidationResult = issueService.validateTransition(applicationUser, parentIssue.getId(), transitionId, issueInputParameters,transopt.build())
    if (transitionValidationResult.isValid()) {
        def transitionResult = issueService.transition(applicationUser, transitionValidationResult)
        log.debug transitionResult.getErrorCollection()
    }
}

