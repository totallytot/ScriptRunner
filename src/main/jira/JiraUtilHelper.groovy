package jira

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.label.LabelManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.mail.Email
import com.atlassian.mail.queue.SingleMailQueueItem
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked

@CompileStatic
@TypeChecked
class JiraUtilHelper {

    static MutableIssue getIssue(String issueKey) {
        ComponentAccessor.issueManager.getIssueObject(issueKey)
    }

    static String getIssueUrl(String issueKey) {
        def baseUrl = ComponentAccessor.applicationProperties.getString("jira.baseurl")
        return "${baseUrl}/browse/${issueKey}"
    }

    static Object getCustomFieldValue(String customFieldName, Issue issue) {
        ComponentAccessor.customFieldManager.getCustomFieldObjects(issue)
                .find { it.name == customFieldName }?.getValue(issue)
    }

    static List<Issue> getIssuesFromJql(ApplicationUser executionUser, String jql) {
        def searchService = ComponentAccessor.getComponentOfType(SearchService)
        def parseResult = searchService.parseQuery(executionUser, jql)
        if (parseResult.valid)
            searchService.search(executionUser, parseResult.query, PagerFilter.unlimitedFilter).results
        else []
    }

    static List<Issue> getIssuesInEpic(Issue epic) {
        ComponentAccessor.issueLinkManager.getOutwardLinks(epic.id).
                findAll { it.issueLinkType.name == "Epic-Story Link" }*.destinationObject
    }

    static def setPriority(ApplicationUser executionUser, MutableIssue issue, String priorityName, Boolean sendEventAndEmail) {
        def priorityToSetId = ComponentAccessor.constantsManager.priorities.find { it.name == priorityName }?.id
        if (!priorityToSetId) return "No such priority"
        if (sendEventAndEmail) {
            def issueService = ComponentAccessor.issueService
            def issueInputParameters = issueService.newIssueInputParameters()
            issueInputParameters.with {
                setSkipScreenCheck(true)
                setPriorityId(priorityToSetId)
            }
            IssueService.UpdateValidationResult validationResult = issueService
                    .validateUpdate(executionUser, issue.id, issueInputParameters)
            if (validationResult.valid) issueService.update(executionUser, validationResult)
            else validationResult.errorCollection
        } else {
            issue.setPriorityId(priorityToSetId)
            ComponentAccessor.issueManager.updateIssue(executionUser, issue,
                    EventDispatchOption.DO_NOT_DISPATCH, false)
        }
    }

    static def setFixVersions(ApplicationUser executionUser, MutableIssue issue, Long ... versionIds) {
        def issueService = ComponentAccessor.issueService
        def issueInputParameters = issueService.newIssueInputParameters()
        issueInputParameters.with {
            setSkipScreenCheck(true)
            setFixVersionIds(versionIds)
        }
        IssueService.UpdateValidationResult validationResult = issueService
                .validateUpdate(executionUser, issue.id, issueInputParameters)
        if (validationResult.valid) issueService.update(executionUser, validationResult)
        else validationResult.errorCollection
    }

    static def setNumberFieldValue(ApplicationUser executionUser, Issue issue, CustomField customField, Number value) {
        def issueService = ComponentAccessor.issueService
        def issueInputParameters = issueService.newIssueInputParameters()
        issueInputParameters.with {
            setSkipScreenCheck(true)
            addCustomFieldValue(customField.idAsLong, value as String)
        }
        IssueService.UpdateValidationResult validationResult = issueService
                .validateUpdate(executionUser, issue.id, issueInputParameters)
        if (validationResult.valid) issueService.update(executionUser, validationResult)
        else validationResult.errorCollection
    }

    static def setSingleSelectListValue(Issue issue, String value, CustomField customField, ApplicationUser executionUser) {
        def optionToSelect = ComponentAccessor.optionsManager.getOptions(customField.getRelevantConfig(issue))
                .find { it.value == value }
        def issueService = ComponentAccessor.issueService
        def issueInputParameters = issueService.newIssueInputParameters()
        issueInputParameters.with {
            setSkipScreenCheck(true)
            addCustomFieldValue(customField.idAsLong, optionToSelect.optionId as String)
        }
        IssueService.UpdateValidationResult validationResult = issueService
                .validateUpdate(executionUser, issue.id, issueInputParameters)
        if (validationResult.valid) issueService.update(executionUser, validationResult)
        else validationResult.errorCollection
    }

    static setIssueLabels(ApplicationUser executionUser, String label, Issue issue) {
        def labelManager = ComponentAccessor.getComponentOfType(LabelManager)
        def existingLabels = labelManager.getLabels(issue.id)*.label
        def labelsToSet = (existingLabels + label).toSet()
        def sendNotification = false
        def issueUpdateEventAndReindex = true
        labelManager.setLabels(executionUser, issue.id, labelsToSet, sendNotification, issueUpdateEventAndReindex)
    }

    // not tested
    static setMultiUserPicker(ApplicationUser executionUser, Issue issue, String fieldName, String... userKeys) {
        def multiUserPicker = ComponentAccessor.customFieldManager.getCustomFieldObject(fieldName)
        def issueService = ComponentAccessor.issueService
        def issueInputParameters = issueService.newIssueInputParameters()
        issueInputParameters.addCustomFieldValue(multiUserPicker.id, userKeys).setSkipScreenCheck(true)
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

    static boolean sendMail(String recipientAddress, String subject, String body) {
        def mailServer = ComponentAccessor.mailServerManager.defaultSMTPMailServer
        def wasAddedToQueue = false
        if (mailServer) {
            def mail = new Email(recipientAddress)
            mail.setSubject(subject)
            mail.setBody(body)
            mail.setMimeType("text/html")
            def item = new SingleMailQueueItem(mail)
            ComponentAccessor.mailQueue.addItem(item)
            wasAddedToQueue = true
        }
        return wasAddedToQueue
    }
}