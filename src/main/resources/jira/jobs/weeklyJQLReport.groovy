package jira.jobs

import com.atlassian.jira.component.ComponentAccessor
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.riadalabs.jira.plugins.insight.channel.external.api.facade.ObjectFacade
import jira.JiraUtilHelper
import jira.ReportGenerator

@WithPlugin("com.riadalabs.jira.plugins.insight")
@PluginModule
ObjectFacade objectFacade

final Map insightContractKeys = ["P9127152":"ITSM-12", "P9117225":"ITSM-15"]
final String EXECUTION_USER_NAME = "service_account"

// noinspection GroovyVariableNotAssigned
def getServiceManagerByInsightKey = { String key ->
    def serviceManager = objectFacade.loadObjectBean(key)
    def serviceManagerId = objectFacade.loadObjectAttributeBean(serviceManager.id, "Сервис-менеджер").
            objectAttributeValueBeans.first().referencedObjectBeanId
    def serviceManagerObject = objectFacade.loadObjectBean(serviceManagerId)
    def jiraUserKey = objectFacade.loadObjectAttributeBean(serviceManagerObject.id, "Профиль").
            objectAttributeValueBeans.first().textValue
    return ComponentAccessor.userManager.getUserByKey(jiraUserKey)
}
def executionUser = ComponentAccessor.userManager.getUserByName(EXECUTION_USER_NAME)

insightContractKeys.each { k, v ->
    def emailAddress = getServiceManagerByInsightKey(v).emailAddress
    def jql = "project = ${k} and created >= \"-7d\""
    def issues = JiraUtilHelper.getIssuesFromJql(executionUser, jql)
    def report = null
    def subject = null
    if (k == "P9127152") {
        report = ReportGenerator.generateDamageControlReport(issues)
        subject = "Еженедельная справка по оказанию услуг технической поддержки"
    } else if (k == "P9117225") {
        report = ReportGenerator.generateTechnicalReport(issues)
        subject = "Еженедельная справка по ремонтно-восстановительным работам"
    }
    // if (report) JiraUtilHelper.sendMail(emailAddress, subject, report)
}