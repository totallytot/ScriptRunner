package jira.post_functions.transitions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.workflow.TransitionOptions.Builder
import org.apache.log4j.Logger
import org.apache.log4j.Level

def log = Logger.getLogger("check")
log.setLevel(Level.DEBUG)
def task
def epic
def feature
def inlink
def ifEpic
def taskunderepic
def issueLinkManager = ComponentAccessor.getIssueLinkManager()

def allIssuesUnderFeature = []
if (issue.getProjectObject().name!='Master (HOF)' && issue.getProjectObject().name!='EpicsWithoutIssues (HOF)'){
    task=issue
    if (issue.isSubTask()){
    log.debug "issue $issue is subtask"
    task= issue.getParentObject()
        if (checkStatus(issue.getParentObject())){
            log.debug "issue changed to $task and continue"
    		allIssuesUnderFeature.add(task)
        }
        else return    
    }
    def epicCF = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName('Epic Link')
    if (task.getCustomFieldValue(epicCF)!=null)	{
        epic = task.getCustomFieldValue(epicCF)
        if (checkStatus(epic)){
        	allIssuesUnderFeature.add(epic)
            def allIssuesUnderEpic = issueLinkManager.getOutwardLinks(issue.getId()).findAll {link-> link.getIssueLinkType().getId()==10400}
        	for(taskLink in allIssuesUnderEpic){
            	task = taskLink.getDestinationObject()
            	if (checkStatus(task)) {}
            	else return
        	}
    		inlink = issueLinkManager.getInwardLinks(epic.getId()).find {link-> link.getIssueLinkType().getId()==10504}
        	if (inlink!=null) feature = inlink.getSourceObject()        
        }
        else return
    }
}
else {	
    if(issue.getIssueType().name.equals('Epic')){    
        log.debug "$issue is epic"
    	def allIssuesUnderEpic = issueLinkManager.getOutwardLinks(issue.getId()).findAll {link-> link.getIssueLinkType().getId()==10400}
        for(taskLink in allIssuesUnderEpic){
            task = taskLink.getDestinationObject()
            if (checkStatus(task)) {}
            else return
        }
    	inlink = issueLinkManager.getInwardLinks(issue.getId()).find {link-> link.getIssueLinkType().getId()==10504}
    	if (inlink!=null) feature = inlink.getSourceObject()
	}
    else{
		log.debug "$issue is feature"
		return
	}
	
}
if (feature!=null && checkStatus(feature)) allIssuesUnderFeature.add(feature)
for (issuek in allIssuesUnderFeature)
    transitIssue(201,issuek)            

boolean checkStatus(def task){
    if (task.getStatus().name.equals('Resolved')||task.getStatus().name.equals('Verified')||task.getStatus().name.equals('Closed')){
        log.debug "issue $task is $task.status.name"
        return true
    }        
    else {
        log.debug "issue $task is out from condition" 
        return false
    } 
}

def transitIssue (int transitionId, Issue parentIssue){
    def applicationUser = ComponentAccessor.getUserManager().getUserByKey("tech_user")
    def issueService = ComponentAccessor.getIssueService();
    def issueInputParameters = issueService.newIssueInputParameters();    
    def builder =  new Builder()
	def transopt = builder.skipConditions().skipValidators().skipPermissions()
    def transitionValidationResult = issueService.validateTransition(applicationUser, parentIssue.getId(), transitionId, issueInputParameters,transopt.build())  
    if (transitionValidationResult.isValid()) {
        log.debug "$parentIssue sent"
        issueService.transition(applicationUser, transitionValidationResult)
    }
}