package jira.scripted_fields
/*
Not finished yet
 */
import com.atlassian.jira.component.ComponentAccessor
import java.time.LocalDate

def issue = ComponentAccessor.issueManager.getIssueObject("TZZ-3")
LocalDate now = LocalDate.now()
def historyManager = ComponentAccessor.changeHistoryManager
def wasFixedVersionChanged = historyManager.getChangeItemsForField(issue, "Fix Version")
        .any {it.created.toLocalDateTime().toLocalDate().isEqual(now)}

def statusChanges = historyManager.getChangeItemsForField(issue, "status")
        .findAll{it.created.toLocalDateTime().toLocalDate().isEqual(now)}

def wasStatusChanged = statusChanges.any {it.fromString.toLowerCase() in ["open", "blocked", "stopped", "reopened", "in progress"] &&
                it.toString.toLowerCase() in ["verified", "closed", "resolved"]}


