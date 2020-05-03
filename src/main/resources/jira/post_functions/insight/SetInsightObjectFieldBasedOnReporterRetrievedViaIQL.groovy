package jira.post_functions.insight

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption

if (issue.issueType.name == "Инцидент") return

// user with corresponding rights for search and issue update
def executionUser = ComponentAccessor.userManager.getUserByName('Admin')

// search in Insight
ComponentAccessor.jiraAuthenticationContext.setLoggedInUser(executionUser)
def iql = "objectType = Клиент AND \"Код пользователя\" = ${issue.reporter.name}"
def iqlFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ComponentAccessor.getPluginAccessor()
        .getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade"))
def client = iqlFacade.findObjectsByIQL(iql) as List

// load Insight object, find attribute value and load Insight object
def objectFacade = ComponentAccessor.getOSGiComponentInstanceOfType(ComponentAccessor.getPluginAccessor()
        .getClassLoader().findClass("com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade"))
def companyKey = objectFacade.loadObjectAttributeBean(client[0].id, "Компания").objectAttributeValueBeans[0].value
def company = objectFacade.loadObjectBean("ITSM-${companyKey}")
def insightCF = ComponentAccessor.customFieldManager.getCustomFieldObject(10267)

// update issue w/t notification
if (!company || !insightCF) return
def companies = [company]
issue.setCustomFieldValue(insightCF, companies)
ComponentAccessor.issueManager.updateIssue(executionUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false)