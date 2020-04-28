package jira.post_functions.transitions

import com.atlassian.jira.component.ComponentAccessor

def customFieldManager = ComponentAccessor.customFieldManager
def externalTeamToEngage = customFieldManager.getCustomFieldObject("customfield_10232") //Select List (multiple choices) Default value: ?
def dependencyType = customFieldManager.getCustomFieldObject("customfield_10508") //Select List (multiple choices) Default value: ?
def sourceSystem = customFieldManager.getCustomFieldObject("customfield_10503") // Select List (single choice) Default value: ?
def middleware = customFieldManager.getCustomFieldObject("customfield_10505") // Select List (single choice) Default value: ?
def targetSystem = customFieldManager.getCustomFieldObject("customfield_10504") // Select List (single choice) Default value: ?

def userStory = issue
def externalTeamToEngageValues = userStory.getCustomFieldValue(externalTeamToEngage) as String
def dependencyTypeValues = userStory.getCustomFieldValue(dependencyType) as String
def sourceSystemValue = userStory.getCustomFieldValue(sourceSystem) as String
def middlewareValue = userStory.getCustomFieldValue(middleware) as String
def targetSystemValue = userStory.getCustomFieldValue(targetSystem) as String

def defaultValue = String.valueOf((char) 8212)

(externalTeamToEngageValues && externalTeamToEngageValues.indexOf(defaultValue) == -1) ||
        (dependencyTypeValues && dependencyTypeValues.indexOf(defaultValue) == -1) ||
        (sourceSystemValue && sourceSystemValue.indexOf(defaultValue) == -1) ||
        (middlewareValue && middlewareValue.indexOf(defaultValue) == -1) ||
        (targetSystemValue && targetSystemValue.indexOf(defaultValue) == -1)