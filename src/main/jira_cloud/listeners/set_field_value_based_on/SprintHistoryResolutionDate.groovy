package jira_cloud.listeners.set_field_value_based_on

import java.text.SimpleDateFormat

logger.info("Working with ${issue.key}")

// get fields and find id
def customFields = get("/rest/api/2/field")
        .asObject(List)
        .body
        .findAll { (it as Map).custom } as List<Map>

def sprintId = customFields.find { it.name == "Sprint" }?.id
logger.info("Sprint id: ${sprintId}")

// get sprints
def sprintValue = issue.fields[sprintId] as String
logger.info("Sprint value: ${sprintValue}")

// check if issue belongs to sprint named "This week"
def inThisWeekSprint = sprintValue?.contains("This week")
logger.info("This week sprint: ${inThisWeekSprint}")

// check resolution
def resolution = issue.fields["resolution"]?.name
logger.info("Resolution: ${resolution}")

// not resolved issues in "This week" sprint or fixed/done issues
if ((!resolution && inThisWeekSprint) || resolution in ["Fixed", "Done"]) {

    // retrieve history records related to sprint "This week" assigment only
    def history = get("/rest/api/2/issue/${issue.key}?expand=changelog").asObject(Map).body.changelog.histories
    def sprintHistory = history.findAll {
        it.items.any { item ->
            item.toString().contains("field:Sprint") && item.toString().contains("toString:This week")
        }
    }

    // check when issue was last added to "This week" sprint
    def lastScheduleDate = sprintHistory?.max {
        it?.created
    }?.created
    logger.info("Issue was last added to This week sprint : ${lastScheduleDate}")

    // scheduled date is a date of issue creation or date when issue was last added to "This week" sprint
    def formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
    def scheduledDate = lastScheduleDate != null ? formatter.parse(lastScheduleDate) : formatter.parse(issue.fields.created)
    logger.info("ScheduledDate is: ${scheduledDate}")

    // calculate age
    def age = null
    if (!resolution) age = new Date() - scheduledDate
    else {
        def resolutionDate = formatter.parse(issue.fields.resolutiondate)
        logger.info("Age calculation: ${resolutionDate} - ${scheduledDate}")
        age = resolutionDate - scheduledDate
    }
    age = age > 0 ? age : 0
    logger.info("Age: ${age}")

    // update custom field "Age"
    def ageId = customFields.find { it.name == 'Age' }?.id
    logger.info("Age id: ${ageId}")
    put("/rest/api/2/issue/${issue.key}")
            .queryString("overrideScreenSecurity", Boolean.TRUE)
            .header("Content-Type", "application/json")
            .body([
                    fields: [(ageId): age]
            ]).asString()
}