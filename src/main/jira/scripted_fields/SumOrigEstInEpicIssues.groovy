package jira.scripted_fields

import com.atlassian.jira.component.ComponentAccessor

if (issue.issueType.name != "Epic") return
def epicIssues = ComponentAccessor.issueLinkManager.getOutwardLinks(issue.id)
        .findAll { it.issueLinkType.name == "Epic-Story Link" }
        *.destinationObject
if (epicIssues.empty) return
return epicIssues
        .findAll { it.originalEstimate != null }
        .collect { it.originalEstimate }.sum()