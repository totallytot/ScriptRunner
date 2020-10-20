package jira.listeners.set_field_based_on

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.issue.link.IssueLinkCreatedEvent
import JiraUtilHelper

final def exceptionLinks = ["Cloners", "Duplicate"]
final def (projectKey, requestType, manualPriority) = ["TEST", "Системная проблема", "Да"]

def linkCreated = event as IssueLinkCreatedEvent
if (linkCreated.issueLink.issueLinkType.name in exceptionLinks) return
log.info "Triggered by ${linkCreated.issueLink.issueLinkType.name}"

// condition
def sourceIssue = linkCreated.issueLink.sourceObject
def destinationIssue = linkCreated.issueLink.destinationObject
def issue = [sourceIssue, destinationIssue].find { issue ->
    issue.projectObject.key == projectKey && !issue.resolution &&
            JiraUtilHelper.getCustomFieldValue("Тип обращения", issue)?.toString() == requestType &&
            JiraUtilHelper.getCustomFieldValue("Ручная установка приоритета", issue)?.toString() != manualPriority
}
if (!issue) return
log.info "Working with ${issue.key}"

// count linked Incidents and update corresponding field
def executionUser = ComponentAccessor.userManager.getUserByName("automation")
def customFieldManager = ComponentAccessor.customFieldManager
def issueLinkManager = ComponentAccessor.issueLinkManager
def allLinkedIssues = issueLinkManager.getInwardLinks(issue.id).findAll {
    !exceptionLinks.contains(it.issueLinkType.name)
}.collect { it.sourceObject }
allLinkedIssues += issueLinkManager.getOutwardLinks(issue.id).findAll {
    !exceptionLinks.contains(it.issueLinkType.name)
}.collect { it.destinationObject }
def allLinkedIncidents = allLinkedIssues.findAll {
    JiraUtilHelper.getCustomFieldValue("Тип обращения", it)?.toString() == "Инцидент"
}
def amountOfIncidentsField = customFieldManager.getCustomFieldObjects(issue).find {
    it.name == "Количество связанных инцидентов"
}
JiraUtilHelper.setNumberFieldValue(executionUser, issue, amountOfIncidentsField, allLinkedIncidents.size())

// priority rules
def isCrisis = {
    allLinkedIncidents.any { it.priority.name == "Кризисный" } ||
            allLinkedIncidents.size() > 10 ||
            allLinkedIncidents.any { JiraUtilHelper.getCustomFieldValue("Deadline", it) }
}
def isCritical = {
    allLinkedIncidents.any { it.priority.name == "Критический" } || allLinkedIncidents.size() > 5
}
def isHigh = {
    allLinkedIncidents.any { it.priority.name == "Высокий" }
}
def isMedium = {
    allLinkedIncidents.any { it.priority.name == "Средний" }
}
def isLow = {
    allLinkedIncidents.any { it.priority.name == "Низкий" }
}

def priorityToSet = null
if (isCrisis()) priorityToSet = "Кризисный"
else if (isCritical()) priorityToSet = "Критический"
else if (isHigh()) priorityToSet = "Высокий"
else if (isMedium()) priorityToSet = "Средний"
else if (isLow()) priorityToSet = "Низкий"
if (!priorityToSet) return
def mutableIssue = ComponentAccessor.issueManager.getIssueObject(issue.key)
JiraUtilHelper.setPriority(executionUser, mutableIssue, priorityToSet, false)