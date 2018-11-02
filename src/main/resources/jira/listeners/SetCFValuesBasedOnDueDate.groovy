import com.atlassian.jira.component.ComponentAccessor
import java.text.SimpleDateFormat


def issue = event.issue
def dateFormat = new SimpleDateFormat("dd/MMM/YY");
if (issue.getIssueType().getName() == "Epic"){
    if (issue.getDueDate().toString() != null){
        def endDate = dateFormat.format(issue.getDueDate().minus(1))
        updateDateCfWithHistory (endDate, 16912L)
        def epicEstimationField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(17302L)
        def epicEstimationValue = epicEstimationField.getValue(issue)
        def startDate
        if (epicEstimationValue != null ){
            startDate = dateFormat.format(issue.getDueDate().minus(epicEstimationValue.toLong().intValue()))
            updateDateCfWithHistory (startDate, 16911L)
        }
        else{
            startDate = dateFormat.format(issue.getDueDate().minus(2))
            updateDateCfWithHistory (startDate, 16911L)
        }
    }
}

def updateDateCfWithHistory(String stringDate, Long cf){

    def issue = event.issue
    def updateDate = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(cf)
    if (updateDate.getValue(issue) == null)
    {
        def user = "tech_user";
        def applicationUser = ComponentAccessor.getUserManager().getUserByKey(user)
        def issueService = ComponentAccessor.getIssueService();
        def issueInputParameters = issueService.newIssueInputParameters();
        issueInputParameters.addCustomFieldValue(updateDate.getIdAsLong(), stringDate)
        def validationResult = issueService.validateUpdate(applicationUser, issue.getId(), issueInputParameters);
        if (validationResult.isValid()) {
            issueService.update(applicationUser, validationResult).hasWarnings()
        }
    }
}