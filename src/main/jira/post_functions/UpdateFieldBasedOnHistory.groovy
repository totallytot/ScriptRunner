package jira.post_functions

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.history.ChangeItemBean
import java.text.SimpleDateFormat

def issue = issue
def reworkAttempt = 0
List<ChangeItemBean> history = ComponentAccessor.getChangeHistoryManager().getChangeItemsForField(issue, "status")
history.each {
    if ((it.getFromString().equals("QA") || it.getFromString().equals("Proof ready")) && it.getToString().equals("TS Ordered"))  reworkAttempt++
}
if (reworkAttempt == 0){
    updateDateCfWithHistory(12531L,issue)
}
else{
    updateDateCfWithHistory(12532L,issue)
}

static void updateDateCfWithHistory(Long cf, def issue) {

    def user = ComponentAccessor.getUserManager().getUserByName("Admin")
    Date date = new Date()
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/YY")
    String stringDate  = dateFormat.format(new Date(date.getTime()))
    IssueService issueService = ComponentAccessor.getIssueService()
    def issueInputParameters = issueService.newIssueInputParameters()
    issueInputParameters.addCustomFieldValue(cf, stringDate)
    IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(user, issue.getId(), issueInputParameters)
    if (validationResult.isValid()) {
        issueService.update(user, validationResult)
    }
}
