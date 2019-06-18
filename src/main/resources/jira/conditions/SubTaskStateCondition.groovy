package jira.conditions

/**
 * Subtask 6 and 7 cannot be put into ready status until subtasks 1-5 are in finished status.
 */

if (issue.issueType.name == "Sub-task2")
    passesCondition = issue.parentObject.subTaskObjects.take(5).every {it.status.name == "Finished"}