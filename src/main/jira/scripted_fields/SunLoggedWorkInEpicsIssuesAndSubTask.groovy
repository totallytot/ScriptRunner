package jira.scripted_fields

import com.atlassian.jira.component.ComponentAccessor

def issuesRelatedToEpic = ComponentAccessor.issueLinkManager.getOutwardLinks(issue.id)
        .findAll { it.issueLinkType.name == "Epic-Story Link" }
        *.destinationObject
def epicSubTasks = issue.subTaskObjects
if (!epicSubTasks.empty) issuesRelatedToEpic += epicSubTasks
def loggedWork = issuesRelatedToEpic.findResults { it.timeSpent }?.sum()