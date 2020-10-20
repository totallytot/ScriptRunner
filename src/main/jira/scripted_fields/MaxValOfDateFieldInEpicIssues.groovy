package jira.scripted_fields

import com.atlassian.jira.component.ComponentAccessor
import java.sql.Timestamp

if (issue.issueType.name != "Epic") return
def epicIssues = ComponentAccessor.issueLinkManager.getOutwardLinks(issue.id)
        .findAll { it.issueLinkType.name == "Epic-Story Link" }
        *.destinationObject
if (!epicIssues || epicIssues.empty) return
def actualEndDate = ComponentAccessor.customFieldManager.customFieldObjects
        .find { it.name == "Actual End Date" }
def allEndDateVals = epicIssues
        .findAll { actualEndDate.getValue(it) != null }
        .collect { actualEndDate.getValue(it) as Timestamp}
if (!allEndDateVals || allEndDateVals.empty) return
Collections.sort(allEndDateVals)
return allEndDateVals.last()