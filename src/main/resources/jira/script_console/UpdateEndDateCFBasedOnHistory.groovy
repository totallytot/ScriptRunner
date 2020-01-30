package jira.script_console

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueInputParameters
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.history.ChangeItemBean
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter

import java.sql.Timestamp
import java.text.SimpleDateFormat

String user = "tech_user";
String jqlQuery = "project = 'TEST : TESTstructure' and issuekey = TEST-26766";
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

        Timestamp lastEndDate = new Timestamp(0L);
        Timestamp transitionDate = null;
        ChangeHistoryManager changeHistoryManager = ComponentAccessor.getChangeHistoryManager();
        List<ChangeItemBean> history = changeHistoryManager.getChangeItemsForField(issue, "status");

        history.each {
            if (it.getToString().equals("Done") || it.getToString().equals("Closed") || it.getToString().equals("Resolved")) {
                transitionDate = it.getCreated();
                if (transitionDate.after(lastEndDate)) lastEndDate = transitionDate;
            }
        }

        if (lastEndDate.getTime() != new Timestamp(0L).getTime()) updateDateCfWithHistory(lastEndDate, issue, applicationUser);
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

