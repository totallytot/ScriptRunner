package jira.escalation_services

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.index.IssueIndexingService
import com.atlassian.jira.util.ImportUtils

final String SERVICE_ACCOUNT = "jira"
final String AFFECTED_FIELD_NAME = "field"

def issueManager = ComponentAccessor.issueManager
def executionUser = ComponentAccessor.userManager.getUserByKey(SERVICE_ACCOUNT)
def customField = ComponentAccessor.customFieldManager.getCustomFieldObjects(issue).find { it.name == AFFECTED_FIELD_NAME }

if (!customField || !executionUser) return

issue.setCustomFieldValue(customField, null)
issueManager.updateIssue(executionUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
def isIndexing = ImportUtils.isIndexIssues()
ImportUtils.setIndexIssues(true)
ComponentAccessor.getComponent(IssueIndexingService.class).reIndex(issue)
ImportUtils.setIndexIssues(isIndexing)