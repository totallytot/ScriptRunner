package jira.post_functions

import com.atlassian.jira.component.ComponentAccessor
import org.apache.log4j.Level
import org.apache.log4j.Logger

def issue = ComponentAccessor.issueManager.getIssueObject("PFT-29")
def log = Logger.getLogger("check-me")
log.setLevel(Level.DEBUG)

if (issue.issueType.name == "Story") {
    def epicIssueTypeId = ComponentAccessor.issueTypeSchemeManager.getIssueTypesForProject(issue.projectObject)
            .find{it.name == "Epic"}.id
    if (epicIssueTypeId) {
        def user = ComponentAccessor.jiraAuthenticationContext.loggedInUser
        def issueService = ComponentAccessor.issueService
        def issueInputParameters = issueService.newIssueInputParameters()
        issueInputParameters.setIssueTypeId(epicIssueTypeId)
        def updateValidationResult = issueService.validateUpdate(user, issue.id, issueInputParameters)
        if (updateValidationResult.valid) issueService.update(user, updateValidationResult)
        else log.debug(updateValidationResult.errorCollection.errors)
    }
}