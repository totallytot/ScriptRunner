package jira.REST_endpoints.reports_with_gui

import com.atlassian.jira.component.ComponentAccessor
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonSlurper
import groovy.transform.BaseScript
import jira.JiraUtilHelper
import jira.ReportGenerator
import org.apache.log4j.Level
import org.apache.log4j.Logger

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response

def logger = Logger.getLogger("reports")
logger.setLevel(Level.DEBUG)

@BaseScript CustomEndpointDelegate delegate

final String EXECUTION_USER_NAME = "service_account"
def userManager = ComponentAccessor.userManager

reportGen(httpMethod: "POST", groups: ["jira-administrators", "reports"]) { MultivaluedMap queryParams, String body ->
    def jsonParser = new JsonSlurper()
    def dataObject = jsonParser.parseText(body) as Map
    def startDate = dataObject.srartDate
    def endDate = dataObject.endDate
    def recipient = dataObject.recipient as String
    def reportType = dataObject.reportType as String

    def recipientMail = userManager.getUserByName(recipient).emailAddress
    def projectKey = reportType == "tech-support" ? "P9117225" : "P9127152"
    def jql = "project = ${projectKey} and created >= ${startDate} and created <= ${endDate}"

    if (reportType == "tech-support") {
        logger.debug reportType
        def issues = JiraUtilHelper.getIssuesFromJql(userManager.getUserByName(EXECUTION_USER_NAME), jql)
        if (issues.empty) {
            logger.debug "jql search is empty"
            logger.debug jql
            return Response.status(502).build()
        }
        def report = ReportGenerator.generateTechnicalReport(issues)
        JiraUtilHelper.sendMail(recipientMail, "Справка по оказанию услуг технической поддержки", report)
        return Response.noContent().build()

    } else if (reportType == "damage-control") {
        logger.debug reportType
        def issues = JiraUtilHelper.getIssuesFromJql(userManager.getUserByName(EXECUTION_USER_NAME), jql)
        if (issues.empty) {
            logger.debug "jql search is empty"
            logger.debug jql
            return Response.status(502).build()
        }
        def report = ReportGenerator.generateDamageControlReport(issues)
        JiraUtilHelper.sendMail(recipientMail, "Справка по ремонтно-восстановительным работам", report)
        return Response.noContent().build()

    } else Response.serverError().build()
}