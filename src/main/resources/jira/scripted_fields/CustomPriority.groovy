package jira.scripted_fields

import com.atlassian.jira.component.ComponentAccessor

def customFieldObjects = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
def impactVal = customFieldObjects.find { it.id == "customfield_11813"}?.getValue(issue) as String
def customerProbabilityVal = customFieldObjects.find { it.id == "customfield_11814"}?.getValue(issue) as String
def overrideVal = customFieldObjects.find { it.id == "customfield_11817"}?.getValue(issue) as String
def specialCareVal = customFieldObjects.find { it.id == "customfield_11821"}?.getValue(issue) as String
def priority = null

if (overrideVal) priority = overrideVal
else if (impactVal && customerProbabilityVal) {
    switch (impactVal) {
        case "Scheduling Logic Failure":
            if (customerProbabilityVal == "All the time") priority = "P0"
            else if (customerProbabilityVal == "Most of the time") priority = "P1"
            else if (customerProbabilityVal == "Occasionally" || customerProbabilityVal == "Rare") priority = "P2"
            break
        case "Crash, Data Loss, or Corruption":
            if (customerProbabilityVal == "All the time" || customerProbabilityVal == "Most of the time") priority = "P0"
            else if (customerProbabilityVal == "Occasionally") priority = "P1"
            else if (customerProbabilityVal == "Rare") priority = "P2"
            break
        case "Performance":
            if (customerProbabilityVal == "All the time") priority = "P1"
            else if (customerProbabilityVal == "Most of the time") priority = "P2"
            else if (customerProbabilityVal == "Occasionally" || customerProbabilityVal == "Rare") priority = "P3"
            break
        case "Security":
            if (customerProbabilityVal == "All the time" || customerProbabilityVal == "Most of the time") priority = "P0"
            else if (customerProbabilityVal == "Occasionally" || customerProbabilityVal == "Rare") priority = "P1"
            break
        case "UI - Low (Bad Phrasing/Typo)":
            if (customerProbabilityVal == "All the time" || customerProbabilityVal == "Most of the time") priority = "P2"
            else if (customerProbabilityVal == "Occasionally" || customerProbabilityVal == "Rare") priority = "P3"
            break
        case "UI - High":
            if (customerProbabilityVal == "All the time") priority = "P0"
            else if (customerProbabilityVal == "Most of the time") priority = "P1"
            else if (customerProbabilityVal == "Occasionally") priority = "P2"
            else if (customerProbabilityVal == "Rare") priority = "P3"
            break
        case "General":
            if (customerProbabilityVal == "All the time") priority = "P0"
            else if (customerProbabilityVal == "Most of the time") priority = "P1"
            else if (customerProbabilityVal == "Occasionally") priority = "P2"
            else if (customerProbabilityVal == "Rare") priority = "P3"
            break
    }
}

if (specialCareVal && priority) {
    switch (specialCareVal) {
        case "Obligated":
            if (priority in ["P0", "P1", "P2"]) priority = "P0"
            else if (priority == "P3") priority = "P1"
            break
        default:
            if (priority in ["P1", "P2", "P3"]) priority = "P3"
            else if (priority == "P0") priority = "P2"
            break
    }
}
return priority