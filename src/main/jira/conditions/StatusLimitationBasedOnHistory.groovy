package jira.conditions

import com.atlassian.jira.component.ComponentAccessor

/**Used for status limitation on "Allow all transitions" transition.
 * Example of usage:
 * Status "Review before CE" has "allow all' transition;
 * Condition checks whether the issue is in required status (! from Pr., ! from PL, LC Translated);
 * Condition checks whether "Review before CE" status was in issue history;
 * if all passed - allows thw transition.
 */

def changeHistoryManager = ComponentAccessor.changeHistoryManager
passesCondition = (
        changeHistoryManager.getAllChangeItems(issue).any {
            it.field == "status" && "Review before CE" in it.fromValues.values()
        }
                && (issue.getStatus().getName().equals("! from Pr.") ||
                issue.getStatus().getName().equals("! from PL") ||
                issue.getStatus().getName().equals("LC Translated"))
)
