package jira_cloud.listeners.set_field_value_based_on

logger.info("Working with ${issue.key}")

// find field id
def customFields = get("/rest/api/2/field")
        .asObject(List)
        .body
        .findAll { (it as Map).custom } as List<Map>

def detectedByCFId = customFields.find { it.name == "Detected By" }?.id
logger.info("Detected By id: ${detectedByCFId}")

// check field current value
def detectedByValue = issue.fields["detectedByCFId"] as String
logger.info("Detected By value: ${detectedByValue}")

// get reporter's groups
def reporterSelfLink = issue.fields.reporter.self
logger.info("Assignee Self Link: ${reporterSelfLink}")
def groups = get("${reporterSelfLink}&expand=groups").asObject(Map).body.groups?.items?.name as List
logger.info("reporter's groups: ${groups}")

// get labels
def labels = issue.fields.labels as List
logger.info("Labels: ${labels}")

// calculate new field value
def newValue = null
if (labels?.contains("jira_escalated") || groups?.contains("support")) newValue = "Support"
else if (groups?.contains("QA")) newValue = "QA"
else if (groups?.contains("Product")) newValue = "Product"
else if (groups?.contains("R&D")) newValue = "R&D"
else if (groups?.contains("BD")) newValue = "Partners"
else if (groups?.contains("Marketing")) newValue = "Marketing"
else newValue = "Other"

//update issue
if (newValue && detectedByCFId && detectedByValue == null) {
    put("/rest/api/2/issue/${issue.key}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .header("Content-Type", "application/json")
            .body([
                    fields: [(detectedByCFId): newValue]
            ]).asString()
}