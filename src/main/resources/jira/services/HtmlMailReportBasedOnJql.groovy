package jira.services

import com.atlassian.jira.component.ComponentAccessor
import groovy.xml.MarkupBuilder
import jira.JiraUtilHelper

final String JQL = "project = P9127152 and created >= startOfWeek(-1) and created <= endOfWeek(-1)"
final String EXECUTION_USER_NAME = "service_account"
final String RECIPIENT_ADDRESS = "fcukthepop@gmail.com"
final String SUBJECT = "Еженедельная справка по оказанию услуг технической поддержки"

def executionUser = ComponentAccessor.userManager.getUserByName(EXECUTION_USER_NAME)
def issues = JiraUtilHelper.getIssuesFromJql(executionUser, JQL)
def baseurl = ComponentAccessor.applicationProperties.getString("jira.baseurl")
def writer = new StringWriter()
def builder = new MarkupBuilder(writer)
def counter = 1

builder.html {
    head {
        style(type: "text/css", """
    table {
        font-size: 12px;
        border-collapse: collapse;
        width: 100%;
    }
    th {
    	text-align: center;
        padding: 5px;
    }
    td {     
        text-align: left;        
        padding: 3px;
    }
     th, td { 
        border: 1px solid black;     
    }
    """)
    }
    body {
        p """Еженедельная справка по оказанию услуг технической поддержки  СИО ИТС Банка России
        компанией ООО "ТехноСерв АС" по договору 80-11.13"""
        table {
            tr {
                th "#"
                th "№ заявки Исполнителя"
                th "Наименование Главного управления Банка России"
                th "Отделение Банка России"
                th "Должность, Ф.И.О. ЗАКАЗЧИКА обратившегося в Сервисный Центр"
                th "Дата и время обращения к услуги по технической поддержки"
                th "Тип СИО (ДГУ,ИБП, СКВ)"
                th "Краткое описание обращения по оказанию услуги ТП"
                th "Средства обращения(телефон, факс, эл. почта)"
                th "Описание оказанной услуги СЦ"
            }
            issues.each { issue ->
                tr {
                    def organization = JiraUtilHelper.getCustomFieldValue("Организация", issue)
                            .toString().replace("[", "").replace("]", "")
                    def unit = JiraUtilHelper.getCustomFieldValue("Подразделение", issue)
                            .toString().replace("[", "").replace("]", "")
                    def service = JiraUtilHelper.getCustomFieldValue("Услуга", issue)
                            .toString().replace("[", "").replace("]", "")
                    def requestSource = JiraUtilHelper.getCustomFieldValue("Источник оповещения", issue)
                            .toString().replace("[", "").replace("]", "")
                    td counter++
                    td { a href: "${baseurl}/browse/${issue.key}", "${issue.key}" }
                    td organization
                    td unit
                    td "${issue.reporter.displayName}"
                    td "${issue.created.toLocalDateTime()}"
                    td service
                    td "${issue.summary}"
                    td requestSource
                    td "${issue.description}"
                }
            }
        }
    }
}
JiraUtilHelper.sendMail(RECIPIENT_ADDRESS, SUBJECT, writer.toString())