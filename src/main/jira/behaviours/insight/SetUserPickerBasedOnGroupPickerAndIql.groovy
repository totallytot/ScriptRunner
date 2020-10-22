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
IQLFacade iqlFacade
@PluginModule
ObjectFacade objectFacade

def secondLineScreens = ["P9127152: Экран перехода на вторую линию"]
def workingGroupField = getFieldByName("Рабочая группа")
def groupResponsibleField = getFieldByName("Ответственный по группе")

def setGroupResponsible = {
    if (workingGroupField.value) {
        def iql = """ objectType = "Рабочая группа" and Наименование = "${workingGroupField.value}" """
        // noinspection GroovyVariableNotAssigned
        def workingGroupObject = iqlFacade.findObjects(iql).first() as ObjectBean
        // noinspection GroovyVariableNotAssigned
        def groupResponsibleId = objectFacade.loadObjectAttributeBean(workingGroupObject.id, "Ответственный по группе").
                objectAttributeValueBeans.first().referencedObjectBeanId
        def groupResponsibleObject = objectFacade.loadObjectBean(groupResponsibleId)
        def jiraUserKey = objectFacade.loadObjectAttributeBean(groupResponsibleObject.id, "Профиль").
                objectAttributeValueBeans.first().value as String
        def jiraUser = ComponentAccessor.userManager.getUserByKey(jiraUserKey)
        groupResponsibleField.setFormValue(jiraUser.username)
    } else groupResponsibleField.setFormValue("")
}

if (getFieldScreen().name in secondLineScreens) {
    def insightCfVal = ComponentAccessor.customFieldManager.getCustomFieldObjects(underlyingIssue).
            find { it.name == "Услуга" }?.getValue(underlyingIssue) as String
    if (insightCfVal) {
        switch (insightCfVal) {
            case "[Поддержка и ремонт Системы бесперебойного электропитания (ИБП) (ITSM-4)]":
                workingGroupField.setFormValue("ИБП техническая поддержка")
                break
            case "[Поддержка и ремонт Системы гарантированного электроснабжения (ДГУ) (ITSM-6)]":
                workingGroupField.setFormValue("ДГУ техническая поддержка")
                break
            case "[Поддержка и ремонт Системы кондиционирования воздуха (СКВ) (ITSM-5)]":
                workingGroupField.setFormValue("СКВ техническая поддержка")
                break
        }
    }
    // error
    if (workingGroupField.value.toString() == "Service Desk") {
        workingGroupField.setError("Пожалуйста, измените группу для перехода на вторую линию.")
    } else workingGroupField.clearError()
    setGroupResponsible()
    // edit screen
} else if (!getAction() && underlyingIssue.status.name == "Первая линия") {
    workingGroupField.setHidden(true)
    groupResponsibleField.setHidden(true)
} else
    setGroupResponsible()