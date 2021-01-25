package jira.validators.transition_screens

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption
import com.opensymphony.workflow.InvalidInputException
import webwork.action.ActionContext

def request = ActionContext.request
if (!request) return

def parameterMap = request.parameterMap
switch (issue.issueType.name) {
    case "Bug":
        def fixVersion = parameterMap.get("fixVersions")
        if (!fixVersion)
            throw new InvalidInputException("fixVersions", "Fix Version/s cannot be empty!")
        break
    case "Support Ticket":
        def supportTicketClassificationVal = getCustomFieldValue("Support Ticket Classification", issue) as LazyLoadedOption
        def fixVersionVal = parameterMap.get("fixVersions")
        def resolutionVal = parameterMap.get("resolution") as List
        def resolutionDoneId = ComponentAccessor.constantsManager.resolutions.find { it.name == "Done"}?.id
        if (!fixVersionVal && resolutionVal.first() == resolutionDoneId && supportTicketClassificationVal?.value in ["Bug", "FN", "FP"])
            throw new InvalidInputException("fixVersions", "Fix Version/s cannot be empty!")
        break
}

static Object getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
            .find { it.name == customFieldName }?.getValue(issue)
}