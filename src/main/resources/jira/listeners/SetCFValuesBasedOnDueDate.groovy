import com.atlassian.jira.component.ComponentAccessor
import java.text.SimpleDateFormat
import com.atlassian.jira.issue.Issue;

Issue issue = event.issue
def dateFormat = new SimpleDateFormat("dd/MMM/YY");
if (issue.getIssueType().getName() == "Epic"){
    if (issue.getDueDate().toString() != null){
        def endDate = dateFormat.format(issue.getDueDate().minus(10))
        updateDateCfWithHistory (endDate, 16912L)
        def origEstimate = issue.getOriginalEstimate()
        def startDate
        if (origEstimate != null ){
            origEstimate/=28800
            origEstimate+=10
            startDate = dateFormat.format(issue.getDueDate().minus((int)Math.round(origEstimate)))
            updateDateCfWithHistory (startDate, 16911L)
        }
        else{
            startDate = dateFormat.format(issue.getDueDate().minus(11))
            updateDateCfWithHistory (startDate, 16911L)
        }
    }
}

void updateDateCfWithHistory(String stringDate, Long cf){

    def issue = event.issue
    def updateDate = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(cf)
    if (updateDate.getValue(issue) == null)
    {
        def user = "tech_user"
        def applicationUser = ComponentAccessor.getUserManager().getUserByKey(user)
        def issueService = ComponentAccessor.getIssueService()
        def issueInputParameters = issueService.newIssueInputParameters()
        issueInputParameters.addCustomFieldValue(updateDate.getIdAsLong(), stringDate)
        def validationResult = issueService.validateUpdate(applicationUser, issue.getId(), issueInputParameters)
        if (validationResult.isValid()) {
            issueService.update(applicationUser, validationResult).hasWarnings()
        }
    }
}