package jira.listeners

import com.atlassian.jira.issue.label.Label
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.customfields.option.Option
import com.atlassian.jira.issue.fields.config.FieldConfig
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.user.ApplicationUser

String user = "user";
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(user);
JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
jiraAuthenticationContext.setLoggedInUser(applicationUser);

//for testing in order to catch the issue
IssueManager issueManager = ComponentAccessor.getIssueManager();
Issue issue = issueManager.getIssueObject("TEST-26807");


//MutableIssue issue = (MutableIssue) event.getIssue();
Set<Label> labels = issue.getLabels();


if (labels.size() > 0) {

    Map<String, String> data = new HashMap<>();
    data.put("AB", "Angry Beavers");
    data.put("dt", "Dream team");
    data.put("BSTeam", "Brain Squad");
    data.put("BotBotB", "Fast&Furious");
    data.put("fastFurious", "Fast&Furious");
    data.put("RDS", "Refactoring Dark Side");
    data.put("HP", "Hypersonic Pancakes"); //project in (CCS, CCH) AND labels = HP
    data.put("FF4", "Team 42");
    data.put("mess", "Man Eating Squirels");
    data.put("ManEatingSquirrels", "Man Eating Squirels");

    labels.each{
        String labelName = it.getLabel();
        data.each { label, team ->

            if(labelName.equals(label)){

                CustomField bpTeam = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(18800L);
                FieldConfig fieldConfig = bpTeam.getRelevantConfig(issue);

                Option value = ComponentAccessor.optionsManager.getOptions(fieldConfig)?.find {
                    it.toString() == team
                }

                bpTeam.updateValue(null, issue, new ModifiedValue(bpTeam.getValue(issue), value), new DefaultIssueChangeHolder())
                return
            }
        }
    }
}

/**

 Fill the "BP Team" in all issue types based on the labels as defined in the attached excel file (CCS project)


 */