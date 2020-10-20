package jira.listeners

import com.atlassian.jira.issue.Issue
import com.atlassian.jira.component.ComponentAccessor

def underlyingIssue = event.issue
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def bp_team = customFieldManager.getCustomFieldObject(18800L)
def fieldConfig = bp_team.getRelevantConfig(underlyingIssue)

if (bp_team.getValue(underlyingIssue) == null) {

    if (underlyingIssue.getIssueType().name == "Epic") {

        SetValue(underlyingIssue, 18800L)

    } else {
        if (underlyingIssue.getIssueType().name != "Roadmap Feature") {

            def linkManager = ComponentAccessor.getIssueLinkManager()
            def storyCollection = linkManager.getInwardLinks(underlyingIssue.id)   //get parent

            if (storyCollection != null) {

                def parentIssue = storyCollection[0].getSourceObject()

                CopyValue(parentIssue, underlyingIssue, 18800L, 18800L)

            }
        }
    }
}

def CopyValue(Issue outIssue, Issue inIssue, Long outField, Long inField) {


    def optionsManager = ComponentAccessor.getOptionsManager()
    String user = "tech_user";
    def applicationUser = ComponentAccessor.getUserManager().getUserByKey(user)
    def issueService = ComponentAccessor.getIssueService()
    def customFieldManager = ComponentAccessor.getCustomFieldManager()
    def outFieldObj = customFieldManager.getCustomFieldObject(outField)
    def inFieldObj = customFieldManager.getCustomFieldObject(inField)
    def outFieldValue = outIssue.getCustomFieldValue(outFieldObj)
    def fieldConfig = inFieldObj.getRelevantConfig(inIssue)
    def value = ComponentAccessor.optionsManager.getOptions(fieldConfig)?.find {
        it.toString() == outFieldValue.toString()
    }
    def issueInputParam = issueService.newIssueInputParameters()
    issueInputParam.addCustomFieldValue(inField, value.getOptionId().toString())
    def validatedResult = issueService.validateUpdate(applicationUser, inIssue.getId(), issueInputParam)

    if (validatedResult.isValid()) {

        def result = issueService.update(applicationUser, validatedResult)
    }
}

def SetValue(Issue inIssue, Long inField) {

    def userUtil = ComponentAccessor.getUserUtil()
    def groups = userUtil.getGroupsForUser(inIssue.getReporter().getName())

    Map<String, String> keys = new HashMap()
    keys.put("infra.Riders", "Infra - Riders")
    keys.put("infra.FizzyBubblech", "Infra - Fizzy Bubblech")
    keys.put("infra.Springularity", "Infra - Springularity")
    keys.put("infra.Llamas", "Infra - Victorious Steel Llamas")
    keys.put("infra.Mossad", "Infra - Mossad")
    keys.put("infra.Cellardoor", "Infra - Cellardoor")
    keys.put("infra.Parsley", "Infra - Mysterious tufts of Parsley")
    keys.put("infra.MobyDick", "Infra - Moby Dick")
    keys.put("infra.HouseStack", "Infra - House of Stack")
    keys.put("infra.SleepingThreads", "Infra - Sleeping Threads")
    keys.put("infra.Hammerhood", "Infra - Hammerhood")
    keys.put("infra.Bamboleo", "Infra - Bamboleo")
    keys.put("Infra.tnt", "Infra - TNT")
    keys.put("infra.Skynet", "Infra - Skynet")
    def value
    groups.each {

        def oneGroup = it.getName()

        keys.each { k, v ->
            if (oneGroup.equals(k)) {

                def customFieldManager = ComponentAccessor.getCustomFieldManager()
                def inFieldObj = customFieldManager.getCustomFieldObject(inField)
                def fieldConfig = inFieldObj.getRelevantConfig(inIssue)
                value = ComponentAccessor.optionsManager.getOptions(fieldConfig)?.find { it.toString() == v }
                String user = "tech_user";
                def applicationUser = ComponentAccessor.getUserManager().getUserByKey(user)
                def issueService = ComponentAccessor.getIssueService()
                def issueInputParam = issueService.newIssueInputParameters()
                issueInputParam.addCustomFieldValue(inField, value.getOptionId().toString())
                def validatedResult = issueService.validateUpdate(applicationUser, inIssue.getId(), issueInputParam)

                if (validatedResult.isValid()) {

                    def result = issueService.update(applicationUser, validatedResult)
                }
                return
            }

        }
    }
}

