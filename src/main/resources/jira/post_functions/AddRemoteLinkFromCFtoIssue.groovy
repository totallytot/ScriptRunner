package jira.post_functions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder
import com.atlassian.jira.issue.link.RemoteIssueLinkManager
import org.apache.log4j.Level
import org.apache.log4j.Logger

RemoteLinks script = new RemoteLinks()
script.run(issue)

class RemoteLinks {
    Logger log = Logger.getLogger("Create Remote links to Issue")
    long ScriptStartTime = System.currentTimeMillis()
    def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
    def issueManager = ComponentAccessor.getIssueManager()
    def strCFIdIssueLinks = "Issue Links" 
    CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager()
    void run(MutableIssue issue) {
        log.setLevel(Level.DEBUG)
        log.debug("${issue.key} Script started")
        long ScriptStartTime = System.currentTimeMillis()
        CustomField linksCF = customFieldManager.getCustomFieldObjectsByName(strCFIdIssueLinks).iterator().next()
        def stringlinks =  issue.getCustomFieldValue(linksCF)
        def links = stringlinks.split()
        for (slink in links){
            if(slink != "")
            {
                try{
                    def linkBuilder = new RemoteIssueLinkBuilder()
                    linkBuilder.issueId(issue.id)
                    linkBuilder.relationship("ECM")
                    linkBuilder.title("Vendor site")
                    linkBuilder.url(slink)
                    def link = linkBuilder.build() 
                    ComponentAccessor.getComponent(RemoteIssueLinkManager).createRemoteIssueLink(link, user)
                }catch (Exception ex) {}
            }
        }
        long ScriptWorkTime = System.currentTimeMillis() - ScriptStartTime
        log.debug("${issue.key} Script work time: ${ScriptWorkTime} ms.")
    }
}