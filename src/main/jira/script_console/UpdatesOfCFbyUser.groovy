package jira.script_console

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.changehistory.ChangeHistoryItem
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager
import com.atlassian.jira.user.ApplicationUser

String affectedUser = "user";
String affectedField = "Team";

List<String> users = new ArrayList<>();
users.add(affectedUser);

ApplicationUser user = ComponentAccessor.getUserManager().getUserByKey(affectedUser);
ChangeHistoryManager changeHistoryManager = ComponentAccessor.getChangeHistoryManager();
List<Issue> issuesUpdatedByUser  = (List<Issue>) changeHistoryManager.findUserHistory(user, users, 10000);
Set<String> resultIssues = new HashSet<>();

issuesUpdatedByUser.each { issue ->
    List<ChangeHistoryItem>	cih = changeHistoryManager.getAllChangeItems(issue);
    cih.each {
        if (it.getUserKey().equals(affectedUser) && it.getField().equals(affectedField)) {
            resultIssues.add(issue.getKey());
            return;
        }
    }
}

