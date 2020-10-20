package jira.scripted_fields

import com.atlassian.jira.component.ComponentAccessor

if (issue.issueType.name != "Epic") return
def epicIssues = ComponentAccessor.issueLinkManager.getOutwardLinks(issue.id)
        .findAll { it.issueLinkType.name == "Epic-Story Link" }
        *.destinationObject
if (!epicIssues || epicIssues.empty) return
def actualEffortCF = ComponentAccessor.customFieldManager.customFieldObjects
        .find { it.name == "Actual Effort" }

def allActualEffortVals = epicIssues
        .findAll { actualEffortCF.getValue(it) != null }
        .collect { actualEffortCF.getValue(it) }

if (!allActualEffortVals || allActualEffortVals.empty) return
allActualEffortVals.sum()