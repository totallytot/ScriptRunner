package jira.listeners

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.label.Label
import com.atlassian.jira.issue.customfields.option.Option
import com.atlassian.jira.issue.fields.config.FieldConfig
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.user.ApplicationUser

/**
 Fill the "BP Team" in all issue types based on the labels as defined in the attached excel file (CCS project)
 */

String user = "tech_user"
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(user)

MutableIssue issue = (MutableIssue) event.getIssue()
Set<Label> labels = issue.getLabels()


if (labels != null && labels.size() > 0) {

    Map<String, String> data = new HashMap<>()
    data.put("AB", "Angry Beavers")
    data.put("dt", "Dream team")
    data.put("BSTeam", "Brain Squad")
    data.put("BotBotB", "Fast&Furious")
    data.put("fastFurious", "Fast&Furious")
    data.put("RDS", "Refactoring Dark Side")
    data.put("HP", "Hypersonic Pancakes") //project in (CCS, CCH) AND labels = HP
    data.put("FF4", "Team 42")
    data.put("mess", "Man Eating Squirels")
    data.put("ManEatingSquirrels", "Man Eating Squirels")

    labels.each{
        String labelName = it.getLabel()
        data.each { label, team ->

            if(labelName.equals(label)){

                CustomField bpTeam = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(18800L)
                FieldConfig fieldConfig = bpTeam.getRelevantConfig(issue)

                Option value = ComponentAccessor.optionsManager.getOptions(fieldConfig)?.find {
                    it.toString() == team
                }

                if (value != null) {

                    IssueService issueService = ComponentAccessor.getIssueService()
                    IssueInputParameters issueInputParameters = issueService.newIssueInputParameters()
                    issueInputParameters.addCustomFieldValue(bpTeam.getIdAsLong(), value.getOptionId().toString())
                    IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(applicationUser, issue.getId(), issueInputParameters)

                    if (validationResult.isValid()) {
                        issueService.update(applicationUser, validationResult)
                        return
                    }
                }
            }
        }
    }
}
