package jira.post_functions.transitions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.workflow.TransitionOptions.Builder
import org.apache.log4j.Logger
import org.apache.log4j.Level

def log = Logger.getLogger("check")
log.setLevel(Level.DEBUG)
log.debug issue.getStatus().name.toString()
def checkFirst = 0
def parentIssue
def transitionId
def issueLinkManager = ComponentAccessor.getIssueLinkManager()
if (issue.isSubTask()){
    parentIssue= issue.getParentObject()
    if (parentIssue.getStatus().getStatusCategory().name.equals('New')){
        transitIssue(271,parentIssue)
        log.debug "task transited"
    }
    else return
}
else{
    if (issue.getProjectObject().name!='Master (HOF)' && issue.getProjectObject().name!='EpicsWithoutIssues (HOF)'){
		def epic = issue.getCustomFieldValue(ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName('Epic Link'))
        if (epic.getStatus().getStatusCategory().name.equals('New')){
            transitIssue(271,epic)
            log.debug "epic transited"
            def inlink = issueLinkManager.getInwardLinks(epic.getId()).find {link-> link.getIssueLinkType().getId()==10504}
            if (inlink!=null) {
                def feature = inlink.getSourceObject()
                if (feature.getStatus().getStatusCategory().name.equals('New')){
                    transitIssue(271,feature)
                    log.debug "feature transited"
                }
            }
        }
    }
    else log.debug "epic or feature"
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