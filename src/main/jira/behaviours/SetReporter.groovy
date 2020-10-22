package jira.behaviours

import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript

import static com.atlassian.jira.issue.IssueFieldConstants.*
@BaseScript FieldBehaviours fieldBehaviours

def reporter = getFieldById(REPORTER)
def requester = getFieldById(fieldChanged)
reporter.setFormValue(requester.value)
reporter.setReadOnly(true)