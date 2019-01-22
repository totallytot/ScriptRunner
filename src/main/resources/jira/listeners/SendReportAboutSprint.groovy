package jira.listeners

import com.atlassian.mail.Email
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.security.JiraAuthenticationContext
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.greenhopper.service.rapid.view.RapidViewService
import com.atlassian.greenhopper.service.sprint.SprintIssueService
import com.atlassian.greenhopper.service.sprint.SprintManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.onresolve.scriptrunner.runner.customisers.JiraAgileBean
import com.onresolve.scriptrunner.runner.customisers.WithPlugin

//def issueManager = ComponentAccessor.getIssueManager()
//def issue = issueManager.getIssueObject("ZEN-116")
//def sprintField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject("Sprint")
//def sprint = sprintField.getValue(issue)

@WithPlugin("com.pyxis.greenhopper.jira")
@JiraAgileBean
RapidViewService rapidViewService
@JiraAgileBean
SprintManager sprintManager
//writer.write("<div id='ghx-chart-selector'>")
//writer.write("<form id='ghx-chart-picker-form' class='aui ghx-chart-picker'>")
//writer.write("<a id='ghx-items-trigger' class='aui-button js-nav-items aui-button-subtle ghx-dropdown-trigger' data-item-id='240' tabindex='0' resolved=''> ZEN 3.10 </a> ")
writer.write("<select id='ghx-chart-picker'> <option value='240' selected=''>ZEN 3.10</option></select></form></div>")

def view = rapidViewService.getRapidView(loggedInUser,context.board).getValue()
def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser()
def sprints = sprintManager.getSprintsForView(view).getValue()

String queryPrep = "Sprint =${sprint.id[0].toString()} and project=${issue.getProjectId()} ORDER BY issuetype DESC"
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser)
def query = jqlQueryParser.parseQuery(queryPrep)
def searchService = ComponentAccessor.getComponent(SearchService)
def jiraAuthenticationContext = ComponentAccessor.getComponent(JiraAuthenticationContext)
def issues = searchService.search(jiraAuthenticationContext.getLoggedInUser(), query, new PagerFilter(0, 99999)).issues
Map <String, List<Issue>> contentForHTML = new HashMap()
for (def i = 0; i< issues.size() - 1; i++){
    if (issues[i]!=null){
        if(contentForHTML.find{ it.key == issues[i].getIssueType().name } ==null )
            contentForHTML.putAt(issues[i].getIssueType().name, new ArrayList())
        contentForHTML.get(issues[i].getIssueType().name).add((Issue)issues[i])
    }
}
contentForHTML.sort()
def mailTo = user.emailAddress
def MailSubject = "Sprint ${sprint[0].name} completed"
String MailBody ="<p>Hi all,</p>"
MailBody = MailBody + "<p>Here is the commitment for ${sprint[0].name} sprint:</p>"
contentForHTML.each{ k, v ->
    MailBody = MailBody + "<p><strong>${k}</strong><br/>"
    v.each{	currentIssue->
        MailBody = MailBody + "&bull; [${currentIssue.getKey()}] - ${currentIssue.summary} <br />"
    }
    MailBody = MailBody + "</p>"
}
def mailServer = ComponentAccessor.getMailServerManager().getDefaultSMTPMailServer()
Email email = new Email(mailTo)
email.setMimeType("text/html")
email.setSubject(MailSubject)
email.setBody(MailBody)
mailServer.send(email)


