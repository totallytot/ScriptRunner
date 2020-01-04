package jira.post_functions

import com.atlassian.jira.component.ComponentAccessor

/*
This snippet was used in JMWE addon as condition for post-function transit parent issue.
Issue type HEC Report is sub-task
 */

def linkedHecReports = ComponentAccessor.issueLinkManager.getOutwardLinks(issue.parentObject.id).findAll {
    it.destinationObject.issueType.name == "HEC Report"}

if (linkedHecReports.size() > 1) {
     if (linkedHecReports.findAll{ it.destinationObject.status.name == "Closed"}.size() == linkedHecReports.size()) true
        else false
} else true

