package jira.listeners

import com.atlassian.jira.component.ComponentAccessor
import java.text.SimpleDateFormat

def issue = event.issue
def dateFormat = new SimpleDateFormat("dd/MMM/YY");
def sprintField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Sprint")
def startSprintDate = dateFormat.format(sprintField.getValue(issue)[0].startDate.toDate())
def endSprintDate  = dateFormat.format(sprintField.getValue(issue)[0].endDate.toDate())
updateDateCfWithHistory (startSprintDate, 16911L)
updateDateCfWithHistory (endSprintDate, 16912L)

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
            issueService.update(applicationUser, validationResult)
        }
    }
}








