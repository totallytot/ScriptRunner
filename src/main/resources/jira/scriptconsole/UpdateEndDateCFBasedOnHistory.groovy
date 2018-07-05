package jira.scriptconsole

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.changehistory.ChangeHistoryItem
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter

import java.sql.Timestamp
import java.text.SimpleDateFormat

String user = "tech_user";
String jqlQuery = "project = 'Infra : Infrastructure' and issuekey = INFRA-26838";
ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(user);

SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
final SearchService.ParseResult parseResult = searchService.parseQuery(applicationUser, jqlQuery);

List<Issue> issues = new ArrayList<>();
if (parseResult.isValid()) {

    final SearchResults results = searchService.search(applicationUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter());
    issues = results.getIssues();
}



if (issues.size() > 0) {

    issues.each {issue ->

        Timestamp lastEndDate = new Timestamp(System.currentTimeMillis());
        ChangeHistoryManager changeHistoryManager = ComponentAccessor.getChangeHistoryManager();
        List<ChangeHistoryItem> history = changeHistoryManager.getAllChangeItems(issue);

        history.each {
            Map<String, String> Tos = it.getTos();
            if (Tos.containsValue("Done") || Tos.containsValue("Closed") || Tos.containsValue("Resolved")) {
                if (lastEndDate.before(it.getCreated())) lastEndDate = it.getCreated();
            }
        }
        updateDateCfWithHistory(lastEndDate, issue, applicationUser);
    }
}

void updateDateCfWithHistory(Timestamp date, Issue issue, ApplicationUser user) {
    CustomField dateField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(16912L);
    if (dateField.getValue(issue) == null)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/YY");
        String stringDate  = dateFormat.format(new Date(date.getTime()));

        IssueService issueService = ComponentAccessor.getIssueService();
        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        issueInputParameters.addCustomFieldValue(dateField.getIdAsLong(), stringDate);
        IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(user, issue.getId(), issueInputParameters);

        if (validationResult.isValid()) {
            issueService.update(user, validationResult);
        }
    }
}