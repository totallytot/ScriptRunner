package jira.REST_endpoints

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.config.database.DatabaseConfigurationManager
import com.atlassian.jira.config.database.JdbcDatasource
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonBuilder
import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.transform.BaseScript

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import java.sql.Driver

/*
Select List Conversions
https://scriptrunner.adaptavist.com/5.6.15/jira/behaviours-conversions.html
 */

@BaseScript CustomEndpointDelegate delegate
// groups: ["jira-software-users"] is used to disable anonymous access
clientPXG(httpMethod: "GET", groups: ["jira-software-users"]) { MultivaluedMap queryParams ->
    def query = queryParams.getFirst("query") as String
    def output = [:]
    def datasource = ComponentAccessor.getComponent(DatabaseConfigurationManager).getDatabaseConfiguration().getDatasource() as JdbcDatasource
    def driver = Class.forName(datasource.getDriverClassName()).newInstance() as Driver
    def properties = new Properties()
    properties.setProperty("user", datasource.getUsername())
    properties.setProperty("password", datasource.getPassword())
    def connection = driver.connect(datasource.getJdbcUrl(), properties)
    def sql = new Sql(connection)
    def mainSqlQuery = "select stringvalue FROM customfieldvalue where customfield = 12000 and issue in (select id from jiraissue where project = 13105)"

    try {
        def rows = sql.rows(mainSqlQuery + "and stringvalue ilike ?", ["%${query}%".toString()] as List)
        output = [
                items : rows.collect { GroovyRowResult row ->
                    [
                            value: row.get("stringvalue"),
                            html : row.get("stringvalue").toString().replaceAll(/(?i)$query/) { String item -> "<b>${item}</b>" },
                            label: row.get("stringvalue"),
                    ]
                },
                total : rows.size(),
                footer: "Choose value... "
        ]
    } finally {
        sql.close()
        connection.close()
    }
    return Response.ok(new JsonBuilder(output).toString()).build();
}

/* Behaviour Initialiser
getFieldByName("Client PXG").convertToMultiSelect([
    ajaxOptions: [
        url : getBaseUrl() + "/rest/scriptrunner/latest/custom/clientPXG",
        query: true,
        formatResponse: "general"
    ]
]) */