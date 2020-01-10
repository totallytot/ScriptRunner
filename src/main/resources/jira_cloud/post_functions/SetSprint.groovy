package jira_cloud.post_functions

logger.info("Working with ${issue.key}")

// get fields and find id
def customFields = get("/rest/api/2/field")
        .asObject(List)
        .body
        .findAll { (it as Map).custom } as List<Map>

def sprintId = customFields.find { it.name == "Sprint" }?.id
logger.info("Sprint id: ${sprintId}")

// get current sprint
def sprintValue = issue.fields[sprintId] as String
logger.info("Sprint value: ${sprintValue}")

if (!sprintValue || (sprintValue && !sprintValue.contains("Urgent Todo"))) {

    // set sprint with id=20 ("This week" sprint)
    put("/rest/api/2/issue/${issue.key}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .header("Content-Type", "application/json")
            .body([
                    fields: [(sprintId): 20]
            ]).asString()
}