package jira.post_functions.insight

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.servicedesk.api.requesttype.RequestTypeService
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade

@WithPlugin("com.atlassian.servicedesk")
@PluginModule RequestTypeService requestTypeService

@WithPlugin("com.riadalabs.jira.plugins.insight")
@PluginModule IQLFacade iqlFacade

final priorityPattern = ~/приоритет запроса:\s*?[1-6]/
final sidPattern = ~/cid:\s*?\d+/
final servicePattern = ~/(?<=сервис клиента:)(.*)/
final contractPattern = ~/(?<=номер договора:)(.*)/

def executionUser = ComponentAccessor.userManager.getUserByName("admin")
ComponentAccessor.jiraAuthenticationContext.setLoggedInUser(executionUser)

//noinspection GroovyVariableNotAssigned
def requestQuery = requestTypeService.newQueryBuilder().issue(issue.id).build()
def requestTypes = requestTypeService.getRequestTypes(executionUser, requestQuery)
def requestTypeName = requestTypes.results[0].name
if (requestTypeName != "Запрос через почту") return
def body = issue.description.toLowerCase()

def priorityMailVal = body.find(priorityPattern)?.find(~/[1-6]/)
if (priorityMailVal) {
    def constantsManager = ComponentAccessor.constantsManager
    def priority = constantsManager.priorities.findByName(priorityMailVal)
    if (priority) issue.setPriority(priority)
    else issue.setPriority(constantsManager.priorities.findByName("3"))
}

def sidMailVal = body.find(sidPattern)?.find(~/\d+/)
def iqlSid = """objectType = Компания AND "CID клиента" = ${sidMailVal}"""
//noinspection GroovyVariableNotAssigned
def companies = iqlFacade.findObjects(iqlSid)
if (!companies.empty) {
    def companyInsightCF = ComponentAccessor.customFieldManager.getCustomFieldObject(10267)
    issue.setCustomFieldValue(companyInsightCF, companies)
}

def serviceMailVal = body.find(servicePattern)?.trim()
def serviceIql = "objectType = Сервис"
def services = iqlFacade.findObjects(serviceIql)
def serviceForUpdate = services.findAll { it.toString().toLowerCase().contains(serviceMailVal) }
if (!serviceForUpdate.empty) {
    def serviceInsightCF = ComponentAccessor.customFieldManager.getCustomFieldObject(10266)
    issue.setCustomFieldValue(serviceInsightCF, serviceForUpdate)
}

def contractMailVal = body.find(contractPattern)?.trim()
def iqlContract = """objectType = Контракт AND Номер = ${contractMailVal}"""
def contracts = iqlFacade.findObjects(iqlContract)
if (!contracts.empty) {
    def contractInsightCF = ComponentAccessor.customFieldManager.getCustomFieldObject(10268)
    issue.setCustomFieldValue(contractInsightCF, contracts)
}

ComponentAccessor.issueManager.updateIssue(executionUser, issue, EventDispatchOption.DO_NOT_DISPATCH, false)