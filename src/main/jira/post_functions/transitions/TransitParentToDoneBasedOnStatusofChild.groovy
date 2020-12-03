package jira.post_functions.transitions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.workflow.TransitionOptions.Builder
import org.apache.log4j.Logger
import org.apache.log4j.Level

def log = Logger.getLogger("check")
log.setLevel(Level.DEBUG)
def checkStatus = 0
def resolvedCount = 0
def verifiedCount = 0
def closedCount = 0
def parentIssue
def issueLinkManager = ComponentAccessor.getIssueLinkManager()
if (issue.isSubTask()){
    parentIssue= issue.getParentObject()
    def allsubtasks = parentIssue.getSubTaskObjects()
    allsubtasks.each{ subik->
        if (subik.getStatus().name.equals('Verified')) checkStatus++
        else if (subik.getStatus().name.equals('Closed')) checkStatus++
    }
    if (checkStatus==allsubtasks.size()&&parentIssue.getStatus().name.equals('In Progress')) {
        transitIssue(261,parentIssue)
        log.debug "task $parentIssue sent"
    }
    else  log.debug "task $parentIssue not complete"
}
else{
    if (issue.getProjectObject().name!='Master (HOF)' && issue.getProjectObject().name!='EpicsWithoutIssues (HOF)'){
        def epic = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName('Epic Link'))
        def linksOfEpic = issueLinkManager.getOutwardLinks(epic.getId())
        def task
        linksOfEpic.each{ issueInEpic->
            task = issueInEpic.getDestinationObject()
            if (task.getStatus().name.equals('Resolved')) resolvedCount++
            else if (task.getStatus().name.equals('Verified')) verifiedCount++
            else if (task.getStatus().name.equals('Closed')) closedCount++
            log.debug "$task $task.status.name checkStatus=$checkStatus resolvedCount=$resolvedCount verifiedCount=$verifiedCount closedCount=$closedCount"
        }
        checkStatus=resolvedCount+verifiedCount+closedCount
        if (checkStatus==linksOfEpic.size())
        {
            if (resolvedCount==0){
                if(verifiedCount==0) transitIssue(241,epic)
                else transitIssue(251,epic)
            }
            else transitIssue(261,epic)
            log.debug "epic $epic sent"
        }
        log.debug "epic $epic not complete"
    }
    else if(issue.getIssueType().name.equals('Epic')){
        def inlink = issueLinkManager.getInwardLinks(issue.getId()).find {link-> link.getIssueLinkType().getId()==10504}
        if (inlink!=null) {
            parentIssue=inlink.getSourceObject()
            def allEpics = issueLinkManager.getOutwardLinks(parentIssue.getId()).findAll {link-> link.getIssueLinkType().getId()==10504}
            def epic
            allEpics.each{epicLink->
                epic = epicLink.getDestinationObject()
                if (epic.getStatus().name.equals('Resolved')) resolvedCount++
                else if (epic.getStatus().name.equals('Verified')) verifiedCount++
                else if (epic.getStatus().name.equals('Closed')) closedCount++
                log.debug "$epic $epic.status.name checkStatus=$checkStatus resolvedCount=$resolvedCount verifiedCount=$verifiedCount closedCount=$closedCount"
            }
            checkStatus=resolvedCount+verifiedCount+closedCount
            if (checkStatus==allEpics.size()) {
                if (resolvedCount==0){
                    if(verifiedCount==0) transitIssue(241,parentIssue)
                    else transitIssue(251,parentIssue)
                }
                else transitIssue(261,parentIssue)
                log.debug "feature $parentIssue sent"
            }
        }
        log.debug "feature $parentIssue not complete"

    }
    else log.debug "feature bb"
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