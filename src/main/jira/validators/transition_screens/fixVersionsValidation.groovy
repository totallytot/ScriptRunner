package jira.validators.transition_screens

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption
import com.opensymphony.workflow.InvalidInputException
import webwork.action.ActionContext

if (!(issue.status.name == "Done" && issue.resolution.name == "Done")) return
def request = ActionContext.request
if (!request) return
def parameterMap = request.parameterMap

def issueType = issue.issueType.name
switch (issueType) {
    case "Bug":
        def fixVersion = parameterMap.get("fixVersions")
        if (!fixVersion)
            throw new InvalidInputException("fixVersions", "Fix Version/s cannot be empty!")
        break
    case "Support Ticket":
        def supportTicketClassificationVal = getCustomFieldValue("Support Ticket Classification", issue) as LazyLoadedOption
        def fixVersion = parameterMap.get("fixVersions")
        if (!fixVersion && supportTicketClassificationVal?.value in ["Bug", "FN", "FP"])
            throw new InvalidInputException("fixVersions", "Fix Version/s cannot be empty!")
        break
}

static Object getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
            .find { it.name == customFieldName }?.getValue(issue)
}