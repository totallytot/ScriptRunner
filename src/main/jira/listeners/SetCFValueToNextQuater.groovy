package jira.listeners

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue

import java.text.SimpleDateFormat

Issue issue = event.issue
def sprintField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Sprint")
def startDate = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(16911L)
Date date = new Date()
if (!issue.isSubTask() && issue.getStatus().getStatusCategory().getName()!="Complete" && sprintField.getValue(issue) == null && startDate.getValue(issue)<date){
    def spField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10002L)
    int sp
    if (spField.getValue(issue)!=null && spField.getValue(issue)!=0) sp = (int)spField.getValue(issue)
    else sp = 1
    Calendar createdDate = Calendar.getInstance()
    createdDate.setTime(issue.getUpdated())
    def month =  createdDate.get(Calendar.MONTH)
    switch (month) {
        case 0:
            setQuater(createdDate,3,sp)
            break;
        case 1:
            setQuater(createdDate,3,sp)
            break;
        case 2:
            setQuater(createdDate,3,sp)
            break;
        case 3:
            setQuater(createdDate,6,sp)
            break;
        case 4:
            setQuater(createdDate,6,sp)
            break;
        case 5:
            setQuater(createdDate,6,sp)
            break;
        case 6:
            setQuater(createdDate,9,sp)
            break;
        case 7:
            setQuater(createdDate,9,sp)
            break;
        case 8:
            setQuater(createdDate,9,sp)
            break;
        case 9:
            setQuater(createdDate,0,sp)
            break;
        case 10:
            setQuater(createdDate,0,sp)
            break;
        case 11:
            setQuater(createdDate,0,sp)
            break;
        default:
            return;
    }

}

void setQuater(Calendar createdDate, int mounthOfQuater, int sp){
    int year =  createdDate.get(Calendar.YEAR)
    if (mounthOfQuater == 0) year++
    def date = new Date(year, mounthOfQuater,1)
    def dateFormat = new SimpleDateFormat("dd/MMM/YY");
    def startDate = dateFormat.format(date)
    def endDate = dateFormat.format(date.plus(sp))
    updateDateCfWithHistory (startDate, 16911L)
    updateDateCfWithHistory (endDate, 16912L)
}


void updateDateCfWithHistory(String stringDate, Long cf){
    def issue = event.issue
    def updateDate = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(cf)
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


/*console version
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.component.ComponentAccessor
import java.text.SimpleDateFormat

Collection<Long> issues = new ArrayList()
List<String> projetsKeys = new ArrayList()
projetsKeys.add("CIT")
projetsKeys.add("MGP")
projetsKeys.add("IRIS")
projetsKeys.each {
    def project = ComponentAccessor.getProjectManager().getProjectObjByKey(it)
    issues.addAll(ComponentAccessor.getIssueManager().getIssueIdsForProject(project.getId()));
}
issues.each {
    Issue issue = ComponentAccessor.getIssueManager().getIssueObject(it)
    def sprintField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName("Sprint")
    def startDate = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(16911L)
    Date date = new Date()
    if (!issue.isSubTask()  && issue.getStatus().getStatusCategory().getName()!="Complete" && sprintField.getValue(issue) == null && (Date)startDate.getValue(issue)<date){
        def spField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(10002L)
        int sp
        if (spField.getValue(issue)!=null && spField.getValue(issue)!=0) sp = (int)spField.getValue(issue)
        else sp = 1
        Calendar createdDate = Calendar.getInstance()
        def month =  createdDate.get(Calendar.MONTH)
        switch (month) {
            case 0:
                setQuater(createdDate,3,sp,issue)
                break;
            case 1:
                setQuater(createdDate,3,sp,issue)
                break;
            case 2:
                setQuater(createdDate,3,sp,issue)
                break;
            case 3:
                setQuater(createdDate,6,sp,issue)
                break;
            case 4:
                setQuater(createdDate,6,sp,issue)
                break;
            case 5:
                setQuater(createdDate,6,sp,issue)
                break;
            case 6:
                setQuater(createdDate,9,sp,issue)
                break;
            case 7:
                setQuater(createdDate,9,sp,issue)
                break;
            case 8:
                setQuater(createdDate,9,sp,issue)
                break;
            case 9:
                setQuater(createdDate,0,sp,issue)
                break;
            case 10:
                setQuater(createdDate,0,sp,issue)
                break;
            case 11:
                setQuater(createdDate,0,sp,issue)
                break;
            default:
                return;
        }

    }

}

void setQuater(Calendar createdDate, int mounthOfQuater, int sp, Issue issue){
    int year =  createdDate.get(Calendar.YEAR)
    if (mounthOfQuater == 0) year++
    def date = new Date(year, mounthOfQuater,1)
    def dateFormat = new SimpleDateFormat("dd/MMM/YY");
    def startDate = dateFormat.format(date)
    def endDate = dateFormat.format(date.plus(sp))
    updateDateCfWithHistory (startDate, 16911L, issue)
    updateDateCfWithHistory (endDate, 16912L, issue)
}


void updateDateCfWithHistory(String stringDate, Long cf, Issue issue){
    def updateDate = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(cf)
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


*/






