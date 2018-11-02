import com.atlassian.jira.component.ComponentAccessor
import java.text.SimpleDateFormat

def issue = event.issue

def sprintField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Sprint")
if (sprintField.getValue(issue) == null){
    Calendar createdDate = Calendar.getInstance()
    createdDate.setTime(issue.getCreated())
        def month =  createdDate.get(Calendar.MONTH)
    switch (month) {
        case 0:
            setQuater(createdDate,3)
            break;
        case 1:
            setQuater(createdDate,3)
            break;
        case 2:
            setQuater(createdDate,3)
            break;
        case 3:
            setQuater(createdDate,6)
            break;
        case 4:
            setQuater(createdDate,6)
            break;
        case 5:
            setQuater(createdDate,6)
            break;
        case 6:
            setQuater(createdDate,9)
            break;
        case 7:
            setQuater(createdDate,9)
            break;
        case 8:
            setQuater(createdDate,9)
            break;
        case 9:
            setQuater(createdDate,0)
            break;
        case 10:
            setQuater(createdDate,0)
            break;
        case 11:
            setQuater(createdDate,0)
            break;
        default:
            return;
    }

}

void setQuater(Calendar createdDate, int mounthOfQuater){
    int year =  createdDate.get(Calendar.YEAR)
    if (mounthOfQuater == 0) year++
    def date = new Date(year, mounthOfQuater,1)
    def dateFormat = new SimpleDateFormat("dd/MMM/YY");
    def startDate = dateFormat.format(date)
    def endDate = dateFormat.format(date.plus(1))
    updateDateCfWithHistory (startDate, 16911L)
    updateDateCfWithHistory (endDate, 16912L)
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