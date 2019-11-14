package jira.validators

import com.opensymphony.workflow.InvalidInputException

def conditionSummary = "Code review for ${issue.key}"
def isNotAllowed = issue.subTaskObjects?.any { it.summary == conditionSummary && it.status.name != "Closed" }
if (isNotAllowed) throw new InvalidInputException("Code review subtask should be closed before transition to QA.")