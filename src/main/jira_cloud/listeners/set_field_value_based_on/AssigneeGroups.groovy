package jira_cloud.listeners.set_field_value_based_on

logger.info("Working with ${issue.key}")

// find field cf "Team" id
def customFields = get("/rest/api/2/field")
        .asObject(List)
        .body
        .findAll { (it as Map).custom } as List<Map>
def teamCFId = customFields.find { it.name == "Team" }?.id
logger.info("Team id: ${teamCFId}")

// check field current value
def teamValue = issue.fields["teamCfId"] as String
logger.info("Issue: ${issue.key}")
logger.info("Team value: ${teamValue}")

// get assignee's groups
def assigneeSelfLink = issue.fields.assignee.self
logger.info("Assignee Self Link: ${assigneeSelfLink}")
def groups = get(assigneeSelfLink + "&expand=groups").asObject(Map).body.groups?.items?.name as List
logger.info("assignee's groups: ${groups}")

// calculate new field value
def newValue = null
if (groups?.contains("Escalations")) newValue = "Escalations"
else if (groups?.contains("Core")) newValue = "Moneytime"
else if (groups?.contains("Experiences")) newValue = "Carma"
else if (groups?.contains("Partners")) newValue = "Prez"
else if (groups?.contains("Onboarding")) newValue = "Onboarding"
else if (groups?.contains("Web")) newValue = "Web"
else if (groups?.contains("Product")) newValue = "Product"
else if (groups?.contains("support")) newValue = "Support"
else if (groups?.contains("QA")) newValue = "QA"
else if (groups?.contains("Infra")) newValue = "Infra"
else if (groups?.contains("BD")) newValue = "BD"
else if (groups?.contains("R&D")) newValue = "R&D"
else newValue = "No data regarding assignee's groups"

// update issue
if (newValue && teamCFId && teamValue == null) {
    put("/rest/api/2/issue/${issue.key}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .header("Content-Type", "application/json")
            .body([
                    fields: [(teamCFId): newValue]
            ]).asString()
}