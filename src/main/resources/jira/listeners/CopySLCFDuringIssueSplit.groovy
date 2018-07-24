package jira.listeners

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.link.IssueLink
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.event.issue.link.IssueLinkCreatedEvent

String user = "tech_user"
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByName(user)

def event = event as IssueLinkCreatedEvent

IssueLink splitLink = null

if (event.getIssueLink().getIssueLinkType().getId() == 11000) splitLink = event.getIssueLink()

CustomField bpTeam = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(18800L)
LazyLoadedOption bpTeamValue = (LazyLoadedOption) splitLink.getSourceObject().getCustomFieldValue(bpTeam)

if (splitLink != null && bpTeamValue != null)
{
    IssueService issueService = ComponentAccessor.getIssueService()
    IssueInputParameters issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters.addCustomFieldValue(bpTeam.getIdAsLong(), bpTeamValue.getOptionId().toString())
    IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(applicationUser, splitLink.getDestinationObject().getId(), issueInputParameters)

    if (validationResult.isValid()) issueService.update(applicationUser, validationResult)
}



