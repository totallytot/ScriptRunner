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
keys.put("TEST.Riders","TEST - Riders" )
keys.put("TEST.FizzyBubblech", "TEST - Fizzy Bubblech" )
keys.put("TEST.Springularity", "TEST - Springularity" )
keys.put("TEST.Llamas", "TEST - Victorious Steel Llamas")
keys.put("TEST.Mossad", "TEST - Mossad" )
keys.put("TEST.Cellardoor", "TEST - Cellardoor" )
keys.put("TEST.Parsley", "TEST - Mysterious tufts of Parsley" )
keys.put("TEST.MobyDick", "TEST - Moby Dick")
keys.put("TEST.HouseStack", "TEST - House of Stack")
keys.put("TEST.SleepingThreads", "TEST - Sleeping Threads" )
keys.put("TEST.Hammerhood", "TEST - Hammerhood" )
keys.put("TEST.Bamboleo", "TEST - Bamboleo" )
keys.put("TEST.tnt", "TEST - TNT" )
keys.put("TEST.Skynet" , "TEST - Skynet" )

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


