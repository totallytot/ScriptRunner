package jira

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.label.LabelManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter

def issue = ComponentAccessor.issueManager.getIssueObject(" ")

static def getCustomFieldValue(String customFieldName, Issue issue) {
    ComponentAccessor.customFieldManager.getCustomFieldObjects(issue).find { it.name == customFieldName }.getValue(issue)
}

static List<Issue> getIssuesFromJql(ApplicationUser executionUser, String jql) {
    def searchService = ComponentAccessor.getComponentOfType(SearchService)
    def parseResult = searchService.parseQuery(executionUser, jql)
    if (parseResult.valid) searchService.search(executionUser, parseResult.query, PagerFilter.unlimitedFilter).results
    else null
}

static List<Issue> getIssuesInEpic(Issue epic) {
    ComponentAccessor.issueLinkManager.getOutwardLinks(epic.id).
            findAll { it.issueLinkType.name == "Epic-Story Link" }*.destinationObject
}

static updateIssueLabels(ApplicationUser executionUser, String label, Issue issue) {
    def labelManager = ComponentAccessor.getComponentOfType(LabelManager)
    def existingLabels = labelManager.getLabels(issue.id)*.label
    def labelsToSet = (existingLabels + label).toSet()
    def sendNotification = false
    def issueUpdateEventAndReindex = true
    labelManager.setLabels(executionUser, issue.id, labelsToSet, sendNotification, issueUpdateEventAndReindex)
}

//not ready
static updateMultiUserPicker(ApplicationUser executionUser, Issue issue, String fieldName, String... userKeys) {
    def multiUserPicker = ComponentAccessor.customFieldManager.getCustomFieldObject(fieldName)
    def issueService = ComponentAccessor.issueService
    def issueInputParameters = issueService.newIssueInputParameters().with {
        addCustomFieldValue(multiUserPicker.id, userKeys).setSkipScreenCheck(true)
    } as IssueInputParameters
    IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(executionUser,
            issue.id, issueInputParameters)
    if (validationResult.valid) issueService.update(executionUser, validationResult)
    else validationResult.errorCollection
}

static clearAssignee(Issue issue) {
    def user = ComponentAccessor.userManager.getUserByName("service_account")
    def issueService = ComponentAccessor.issueService
    def validateAssignResult = issueService.validateAssign(user, issue.id, null)
    if (validateAssignResult.valid) issueService.assign(user, validateAssignResult)
    validateAssignResult.errorCollection
}