package jira.post_functions.insight

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.services.model.MutableObjectBean

@WithPlugin("com.riadalabs.jira.plugins.insight")
@PluginModule
IQLFacade iqlFacade
@PluginModule
ObjectFacade objectFacade

if (issue.issueType.name == "Инцидент") return

// user with corresponding rights for search and issue update
def executionUser = ComponentAccessor.userManager.getUserByName('Admin')

// search in Insight
ComponentAccessor.jiraAuthenticationContext.setLoggedInUser(executionUser)
def iql = "objectType = Клиент AND \"Код пользователя\" = ${issue.reporter.name}"
// noinspection GroovyVariableNotAssigned
def client = iqlFacade.findObjects(iql).first() as MutableObjectBean

// load Insight object, find attribute value and load Insight object
// noinspection GroovyVariableNotAssigned
def companyKey = objectFacade.loadObjectAttributeBean(client.id, "Компания").objectAttributeValueBeans[0].value
def company = objectFacade.loadObjectBean("ITSM-${companyKey}")
def insightCF = ComponentAccessor.customFieldManager.getCustomFieldObject(10267)

// update issue w/t notification
if (!company || !insightCF) return
def companies = [company]
issue.setCustomFieldValue(insightCF, companies)
ComponentAccessor.issueManager.updateIssue(executionUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false)