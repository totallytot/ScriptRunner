package jira.postfunctions.console_tests

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue

def issueManager = ComponentAccessor.issueManager
def issue = issueManager.getIssueObject("HEPROD-5") as MutableIssue

//DTO Approval field (string)
def dto = ComponentAccessor.customFieldManager.getCustomFieldObject(10965l)
def isDtoApproved = Boolean.parseBoolean(dto.getValueFromIssue(issue))

//Technical Support Approval (String)
def ts = ComponentAccessor.customFieldManager.getCustomFieldObject(10966l)
def isTsApproved = Boolean.parseBoolean(ts.getValueFromIssue(issue))

if (isDtoApproved && isTsApproved) {
    def issueService = ComponentAccessor.issueService
    def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
    def param = issueService.newIssueInputParameters()
    IssueService.TransitionValidationResult transitionValidationResult = issueService.
            validateTransition(currentUser, issue.getId(), 51, param)
    if (transitionValidationResult.isValid()) issueService.transition(currentUser, transitionValidationResult)
}