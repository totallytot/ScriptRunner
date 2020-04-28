package jira.post_functions.transitions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor

if (issue.get("customfield_10964")==1){
    IssueService issueService = ComponentAccessor.getIssueService()
    def iIParameters = issueService.newIssueInputParameters();
    IssueService.TransitionValidationResult transitionValidationResult = issueService.validateTransition(currentUser, issue.getId(), 51, iIParameters)
    if (transitionValidationResult.isValid()) {
        IssueService.IssueResult transitionResult = issueService.transition(currentUser, transitionValidationResult)
    }
}
else {
    def issueService = ComponentAccessor.getIssueService()
    def issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters.addCustomFieldValue(10964L,"2")
    def validationResult = issueService.validateUpdate(currentUser, issue.getId(), issueInputParameters)
    if (validationResult.isValid()) {
        issueService.update(currentUser, validationResult).hasWarnings()
    }
}