package jira.conditions

import com.atlassian.jira.component.ComponentAccessor

def issue = ComponentAccessor.issueManager.getIssueObject(" ") //for testing in script console only

def affectedSubTaskSummaries = ["Rule Go Live - Apply Soc Core Tag", "One Week Follow Up",
                                "One Month Follow Up"].collect { it.toLowerCase() }
def conditionSubTaskSummaries = ["Content Approval"].collect { it.toLowerCase() }
def conditionStatus = "Approved"

if (issue.summary.toLowerCase() in affectedSubTaskSummaries) {
    passesCondition = issue.parentObject.subTaskObjects.any {
        it.summary.toLowerCase() in conditionSubTaskSummaries && it.status.name == conditionStatus
    }
}