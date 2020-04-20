package jira.scripted_fields

import com.atlassian.jira.component.ComponentAccessor
import java.sql.Timestamp
import java.text.SimpleDateFormat

if (issue.issueType.name != "Epic") return
def epicIssues = ComponentAccessor.issueLinkManager.getOutwardLinks(issue.id)
        .findAll { it.issueLinkType.name == "Epic-Story Link" }*.destinationObject
if (epicIssues.empty) return
def actualStartDate = ComponentAccessor.customFieldManager.customFieldObjects
        .find { it.name == "Actual Start Date" }
def allStartDateVals = epicIssues
        .findAll { actualStartDate.getValue(it) != null }
        .collect { actualStartDate.getValue(it) as Timestamp}
Collections.sort(allStartDateVals)
return new SimpleDateFormat("dd/MMM/yy").format(allStartDateVals.first())