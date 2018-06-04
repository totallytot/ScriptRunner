package jira.listeners

import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.component.ComponentAccessor

def underlyingIssue = event.issue
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def optionsManager = ComponentAccessor.getOptionsManager()
def userUtil = ComponentAccessor.getUserUtil()

def bp_team = customFieldManager.getCustomFieldObject(18800L)
def fieldConfig = bp_team.getRelevantConfig(underlyingIssue)
def value

def groups = userUtil.getGroupsForUser(underlyingIssue.getAssignee().getName())

Map <String, String> keys = new HashMap()
keys.put("infra.Riders","Infra - Riders" )
keys.put("infra.FizzyBubblech", "Infra - Fizzy Bubblech" )
keys.put("infra.Springularity", "Infra - Springularity" )
keys.put("infra.Llamas", "Infra - Victorious Steel Llamas")
keys.put("infra.Mossad", "Infra - Mossad" )
keys.put("infra.Cellardoor", "Infra - Cellardoor" )
keys.put("infra.Parsley", "Infra - Mysterious tufts of Parsley" )
keys.put("infra.MobyDick", "Infra - Moby Dick")
keys.put("infra.HouseStack", "Infra - House of Stack")
keys.put("infra.SleepingThreads", "Infra - Sleeping Threads" )
keys.put("infra.Hammerhood", "Infra - Hammerhood" )
keys.put("infra.Bamboleo", "Infra - Bamboleo" )
keys.put("Infra.tnt", "Infra - TNT" )
keys.put("infra.Skynet" , "Infra - Skynet" )

groups.each{

    def oneGroup = it.getName()

    keys.each{ k, v ->
        if(oneGroup.equals(k)){

            value = ComponentAccessor.optionsManager.getOptions(fieldConfig)?.find { it.toString() == v}
            bp_team.updateValue(null, underlyingIssue, new ModifiedValue(null, value), new DefaultIssueChangeHolder())
            return
        }
    }
}


