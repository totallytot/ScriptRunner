package jira_cloud.post_functions.set_field_value_based_on

import kong.unirest.Unirest

/**
 * Extract Timestamp, Application, Sub-System, Description from "Description" field and update corresponding fields with extracted data.
 * Source data:
 *  INFO - CB Hunting: Possible Privilege-Escalation By Cron (ATT&CK - T1053.003)
 *  Timestamp: 2020/11/24 12:29:46 GMT
 *  Application: carbon_black
 *  Sub-System: events
 *  Severity: INFO
 *  TeamName: Alpha-Security
 *  Description: Cron - Replace crontab with referenced file
 *  Created By: bob@example.com
 * ### Trigger ###
 *  - issue creation.
 */

final String timeStamp = "Timestamp"
final String application = "Application"
final String subSystem = "Sub-System"
final String description = "Description"

def customFields = customFields
def timeStampId = customFields.find { it.name == timeStamp }?.id
def applicationId = customFields.find { it.name == application }?.id
def subSystemId = customFields.find { it.name == subSystem }?.id
def descriptionId = customFields.find { it.name == description }?.id

def descVal = issue["fields"]["description"]
if (!descVal) return

descVal = (descVal as String).replaceAll("\n", " ").replaceAll("\r", " ")
def fieldsValsMapping = [:]

def timeStampVal = descVal.find("(?<=Timestamp:).*(?=Application:)")?.trim()
fieldsValsMapping[timeStampId] = timeStampVal

def applicationVal = descVal.find("(?<=Application:).*(?=Sub-System:)")?.trim()
fieldsValsMapping[applicationId] = applicationVal

def subSystemVal = descVal.find("(?<=Sub-System:).*(?=Severity:)")?.trim()
fieldsValsMapping[subSystemId] = subSystemVal

def descriptionVal = descVal.find("(?<=Description:).*(?=Created By:)")?.trim()
fieldsValsMapping[descriptionId] = descriptionVal

setFields(issue["key"] as String, fieldsValsMapping)

static List<Map> getCustomFields() {
    Unirest.get("/rest/api/3/field")
            .header('Content-Type', 'application/json')
            .asObject(List)
            .body
            .findAll { (it as Map).custom } as List<Map>
}

static int setFields(String issueKey, Map fieldsValsMapping) {
    def result = Unirest.put("/rest/api/3/issue/${issueKey}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .queryString("notifyUsers", Boolean.FALSE)
            .header("Content-Type", "application/json")
            .body([fields: fieldsValsMapping]).asString()
    return result.status
}