package jira.post_functions.insight

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.servicedesk.api.organization.OrganizationService
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

@WithPlugin("com.atlassian.servicedesk")
@PluginModule
OrganizationService organizationService

// check if reporter is related to a certain company in Insight
def iql = "Пользователь = ${issue.reporter.name}"
// noinspection GroovyVariableNotAssigned
def insightUser = iqlFacade.findObjects(iql).first() as MutableObjectBean
if (!insightUser) return
// noinspection GroovyVariableNotAssigned
def companyId = objectFacade.loadObjectAttributeBean(insightUser.id, "Компания")
        .objectAttributeValueBeans.first().referencedObjectBeanId
if (!companyId) return
def companyName = objectFacade.loadObjectBean(companyId).label
if (companyName != "ТЕЛЕ2") return

def executionUser = ComponentAccessor.userManager.getUserByName("admin")
// noinspection GroovyVariableNotAssigned
def TELE2 = organizationService.getById(executionUser, 120)
def organizationField = ComponentAccessor.customFieldManager.getCustomFieldObject(10002)
def organizationsToAdd = [TELE2]
issue.setCustomFieldValue(organizationField, organizationsToAdd)
ComponentAccessor.issueManager.updateIssue(executionUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false)