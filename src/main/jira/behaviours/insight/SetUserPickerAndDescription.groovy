package jira.behaviours.insight

import com.atlassian.jira.component.ComponentAccessor
import com.onresolve.jira.groovy.user.FieldBehaviours
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

@WithPlugin("com.riadalabs.jira.plugins.insight")
@PluginModule
ObjectFacade objectFacade

def insightCf = getFieldByName("Договор")
def serviceManagerField = getFieldByName("Сервис-менеджер")
def descriptionField = getFieldById("description")
if (insightCf.value) {
    def objectKey = insightCf.value as String
    // noinspection GroovyVariableNotAssigned
    def sppObject = objectFacade.loadObjectBean(objectKey)
    // set Service Manager
    def serviceManagerId = objectFacade.loadObjectAttributeBean(sppObject.id, "Сервис-менеджер").
            objectAttributeValueBeans.first().referencedObjectBeanId
    def serviceManagerObject = objectFacade.loadObjectBean(serviceManagerId)
    def jiraUserKey = objectFacade.loadObjectAttributeBean(serviceManagerObject.id, "Профиль").
            objectAttributeValueBeans.first().value as String
    def jiraUser = ComponentAccessor.userManager.getUserByKey(jiraUserKey)
    serviceManagerField.setFormValue(jiraUser.username)
    // set Description if it is empty
    if (!descriptionField.value) {
        def contractNumber = objectFacade.loadObjectAttributeBean(sppObject.id, "Номер договора").
                objectAttributeValueBeans.first().value
        descriptionField.setFormValue(contractNumber)
    }
} else {
    serviceManagerField.setFormValue("")
    descriptionField.setFormValue("")
}