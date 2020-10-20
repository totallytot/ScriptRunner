package jira_cloud.listeners.set_field_value_based_on

logger.info("Working with ${issue.key}")

//get fields
def customFields = get("/rest/api/2/field")
        .asObject(List)
        .body
        .findAll { (it as Map).custom } as List<Map>

// get labels
def labels = issue.fields.labels as List
logger.info("Labels: ${labels}")

// find value of "Requested by" field (label type)
def requestedByCFId = customFields.find { it.name == "Requested by" }?.id
logger.info("Requested by id: ${requestedByCFId}")
def requestedByValue = issue.fields[requestedByCFId]?.value
logger.info("Requested By value: ${requestedByValue}")

// get reporter's groups
def reporterSelfLink = issue.fields.reporter.self
logger.info("Reporter Self Link: ${reporterSelfLink}")
def groups = get("${reporterSelfLink}&expand=groups").asObject(Map).body.groups?.items?.name as List
logger.info("reporter's groups: ${groups}")

// update field "By Customer"
if (requestedByValue != null || labels.contains("jira_escalated") || groups?.contains("support")) {
    def byCustomerCfId = customFields.find { it.name == "By Customer" }?.id
    put("/rest/api/2/issue/${issue.key}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .header("Content-Type", "application/json")
            .body([
                    fields: [(byCustomerCfId): "True"]
            ]).asString()
}