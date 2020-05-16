package jira.scripted_fields

import com.atlassian.jira.component.ComponentAccessor
import java.sql.Timestamp

if (issue.issueType.name != "Epic") return
def epicIssues = ComponentAccessor.issueLinkManager.getOutwardLinks(issue.id)
        .findAll { it.issueLinkType.name == "Epic-Story Link" }
        *.destinationObject
if (!epicIssues || epicIssues.empty) return
def actualStartDate = ComponentAccessor.customFieldManager.customFieldObjects
        .find { it.name == "Actual Start Date" }
def allStartDateVals = epicIssues
        .findAll { actualStartDate.getValue(it) != null }
        .collect { actualStartDate.getValue(it) as Timestamp}
if (!allStartDateVals || allStartDateVals.empty) return
Collections.sort(allStartDateVals)
return allStartDateVals.first()