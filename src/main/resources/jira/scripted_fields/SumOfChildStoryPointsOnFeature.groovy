package jira.scripted_fields

import com.atlassian.jira.component.ComponentAccessor;
 
def issueLinkManager = ComponentAccessor.getIssueLinkManager()
def totalSP = 0.0
enableCache = {-> false}

 
log.debug("Issue ${issue}")
issueLinkManager.getOutwardLinks(issue.id)?.each {issueLink ->;


    if ((issueLink.issueLinkType.name == "Epic-Story Link")) {  
        customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Story Points")
        def SP = issueLink.destinationObject.getCustomFieldValue(customField) as Double ?: 0.0
        log.debug("SP value ${SP}")
        totalSP += SP
    }}
 
return totalSP as Double
