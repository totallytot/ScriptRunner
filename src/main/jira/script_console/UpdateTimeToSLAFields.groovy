package jira.script_console

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.history.ChangeItemBean
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.component.ComponentAccessor


def query = ComponentAccessor.getComponent(JqlQueryParser).parseQuery("issuetype in (PL-Buch, PL-French, PL-Russian, PL-Spanish, PL-englishBook) AND 'CE start' is not EMPTY and createdDate >=  '2019-01-03'")
def jiraAuthenticationContext = ComponentAccessor.getComponent(JiraAuthenticationContext)
def issues = ComponentAccessor.getComponent(SearchService).search(jiraAuthenticationContext.getLoggedInUser(), query, new PagerFilter(0, 99999)).issues

for (def i = 0 ; i<issues.size();i++){
    def issue = issues[i]
    return issue
    def CELevelF = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11406L)
    def indexF = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11609L)
    def TypeString = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(11409L)
    def ceDeadline = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12546L)
    def tsDeadline = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12545L)
    def valueCE = 0
    def valueTS = 0
    def valueTSRW = 0
    def user = ComponentAccessor.getUserManager().getUserByName("Admin")
    IssueService issueService = ComponentAccessor.getIssueService()
    def issueInputParameters = issueService.newIssueInputParameters()
//calculating valueCE
    if (issue.getProjectObject().name.equals("PELATEST")) valueCE+=3
    if (indexF.getValue(issue)!=null && indexF.getValue(issue).toString().equals("concordance list (Index service level B1)")) valueCE+=3
    if(CELevelF.getValue(issue)!=null){
        switch (CELevelF.getValue(issue).toString()){
            case "CE1":
                valueCE+=6
                break
            case "CE2":
                valueCE+=10
                break
            case "CE3":
                valueCE+=15
                break
            case "CE4":
                valueCE+=25
                break
            case "DTS":
                valueCE=null
                break
        }
    }
    else valueCE=null
//calculating valueTS
    if(TypeString.getValue(issue)!=null){
        if (TypeString.getValue(issue).toString().equals("3")) valueTS=4
        else valueTS=3  }
    if(CELevelF.getValue(issue)!=null && CELevelF.getValue(issue).toString().equals("DTS")) valueTS=8
    if (issue.getProjectObject().name.equals("PELATEST")&& valueTS!=0)  valueTS+=3
//calculating valueTSRW
    def reworkAttempt = 0
    List<ChangeItemBean> history = ComponentAccessor.getChangeHistoryManager().getChangeItemsForField(issue, "status")
    history.each {
        if ((it.getFromString().equals("QA") || it.getFromString().equals("Proof ready")) && it.getToString().equals("TS Ordered"))  reworkAttempt++
    }
    if (reworkAttempt == 1) valueTSRW += 1
    else if (reworkAttempt > 1) valueTSRW += 2
    else if (reworkAttempt == 0) valueTSRW = null
//ts start
    if (ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12530L).getValue(issue)!=null && tsDeadline.getValue(issue)==null && valueTS!=null)
        issueInputParameters.addCustomFieldValue(12536L, valueTS.toString()+"d")
    else issueInputParameters.addCustomFieldValue(12536L, null)
//ce start
    if (ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12528L).getValue(issue)!=null && ceDeadline.getValue(issue)==null && valueCE!=null )
        issueInputParameters.addCustomFieldValue(12535L, valueCE.toString()+"d")
    else issueInputParameters.addCustomFieldValue(12535L, null)
//tsrw start
    if (ComponentAccessor.getCustomFieldManager().getCustomFieldObject(12531L).getValue(issue)!=null && valueTSRW!=null)
        issueInputParameters.addCustomFieldValue(12537L, valueTSRW.toString()+"d")
    else issueInputParameters.addCustomFieldValue(12537L, null)
    issueInputParameters.setSkipScreenCheck(true)
    return issueService.validateUpdate(user, issue.getId(), issueInputParameters)
    IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(user, issue.getId(), issueInputParameters)
    if (validationResult.isValid()) {
        issueService.update(user, validationResult)
    }

}