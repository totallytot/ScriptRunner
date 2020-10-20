package jira.conditions

import com.atlassian.jira.component.ComponentAccessor
import org.apache.log4j.Logger
import org.apache.log4j.Level

if (issue.getProjectObject().name.equals("Testzigzag")) {
    def log = Logger.getLogger("check")
    log.setLevel(Level.DEBUG)
    def issueLinkManager = ComponentAccessor.getIssueLinkManager()
    def condition = true
    if (issue.getIssueType().name == "Task") {
        def blockers = issueLinkManager.getInwardLinks(issue.getId()).findAll { link -> link.getIssueLinkType().getId() == 10000 }
        blockers.each { blockerlink ->
            def blocker = blockerlink.getSourceObject()
            def blockerStatus = blocker.getStatus().name
            log.debug "$blocker is $blockerStatus"
            if (!(blockerStatus.equals('Resolved') || blockerStatus.equals('Verified') || blockerStatus.equals('Closed'))) {
                log.debug "$blocker block transition"
                condition = false
            }
        }
        return condition
    }
} else return true