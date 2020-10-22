package jira.behaviours.insight

import com.atlassian.jira.component.ComponentAccessor
import com.onresolve.jira.groovy.user.FieldBehaviours
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.IQLFacade
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import com.riadalabs.jira.plugins.insight.services.model.ObjectBean
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

@WithPlugin("com.riadalabs.jira.plugins.insight")
@PluginModule
ObjectFacade objectFacade
@PluginModule
IQLFacade iqlFacade

final String EBP = "ИБП техническая поддержка"
final String DGU = "ДГУ техническая поддержка"
final String SKV = "СКВ техническая поддержка"

def insightService = getFieldById(getFieldChanged())
def workingGroup = getFieldByName("Рабочая группа")
def groupResponsible = getFieldByName("Ответственный по группе")
workingGroup.setReadOnly(true)
groupResponsible.setReadOnly(true)

def groupResponsibleUpdate = { String groupName ->
    def iql = "objectType = \"Рабочая группа\" and Наименование = \"${groupName}\""
    // noinspection GroovyVariableNotAssigned
    def insightWorkingGroupObject = iqlFacade.findObjects(iql)[0] as ObjectBean
    if (insightWorkingGroupObject) {
        def groupResponsibleId = objectFacade.loadObjectAttributeBean(insightWorkingGroupObject.id, "Ответственный по группе")
                .objectAttributeValueBeans.first().referencedObjectBeanId
        def groupResponsibleObject = objectFacade.loadObjectBean(groupResponsibleId)
        def jiraUserKey = objectFacade.loadObjectAttributeBean(groupResponsibleObject.id, "Профиль")
                .objectAttributeValueBeans.first().value as String
        def jiraUser = ComponentAccessor.userManager.getUserByKey(jiraUserKey)
        groupResponsible.setFormValue(jiraUser.username)
    }
}

if ((getActionName() == "Create") || !getAction() && underlyingIssue.status.name == "Первая линия") return

def serviceKey = insightService.value as String
log.warn getActionName()
log.warn serviceKey
switch (serviceKey) {
    case "ITSM-4":
    case "ITSM-7":
        workingGroup.setFormValue(EBP)
        groupResponsibleUpdate(EBP)
        break
    case "ITSM-6":
    case "ITSM-9":
        workingGroup.setFormValue(DGU)
        groupResponsibleUpdate(DGU)
        break
    case "ITSM-5":
    case "ITSM-8":
        workingGroup.setFormValue(SKV)
        groupResponsibleUpdate(SKV)
        break
}