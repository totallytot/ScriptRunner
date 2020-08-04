package jira_cloud.scheduled_jobs

import kong.unirest.Unirest

//def requestFields = Unirest.get("/rest/api/2/field").asObject(List)
//assert requestFields.status == 200
//def customFields = requestFields.body.findAll { (it as Map).custom } as List<Map>
//def calcRankId = customFields.find { it.name == "Calculated Rank" }?.id
//assert calcRankId != null
//def requestCountId = customFields.find { it.name == "Requests Count" }?.id
//assert requestCountId != null

logger.info "### Calculated Rank Scheduled Job Started ###"
def calculatedRankCfId = "customfield_12727"
def requestCountCfId = "customfield_12654"
def jqlQuery = 'project = "Connect" and status != "Closed"'
int startAt, jqlOutputItemsCount
def maxResults = 100
def searchResult = []

// workaround for old Groovy version
def keepSearchGoing = true
while (keepSearchGoing) {
    def searchIntermediateResult = executeJqlSearch(jqlQuery, startAt, maxResults)
    searchResult.addAll(searchIntermediateResult)
    jqlOutputItemsCount = searchIntermediateResult.size()
    startAt += maxResults
    keepSearchGoing = jqlOutputItemsCount >= maxResults
}

searchResult.each { item ->
    def issue = item as Map
    def votes = issue.fields.votes.votes as Integer
    def requestCountVal = issue.fields["${requestCountCfId}"] as Integer
    def calculatedRankVal = (requestCountVal * 0.4 + votes * 0.6) as Integer
    logger.info "Actual calculatedRank value is ${calculatedRankVal}"
    def currentCalculatedRankVal = issue.fields[calculatedRankCfId] as Integer
    logger.info "Current calculatedRank value is ${currentCalculatedRankVal}"
    def isUpdateRequired = (calculatedRankVal != currentCalculatedRankVal)
    logger.info "${issue.key} update: ${isUpdateRequired.toString()}"
    if (isUpdateRequired) {
        def result = Unirest.put("/rest/api/2/issue/${issue.key}")
                .queryString("overrideScreenSecurity", Boolean.TRUE)
                .queryString("notifyUsers", Boolean.FALSE)
                .header("Content-Type", "application/json")
                .body([
                        fields: [
                                (calculatedRankCfId): calculatedRankVal
                        ]
                ]).asString()
        if (result.status == 204) logger.info "The ${issue.key} issue was proccessed/updated (204)."
        else logger.warn "Failed to update the ${issue.key} issue. ${result.status}: ${result.body}"
    }
}
logger.info "### Calculated Rank Scheduled Job Ended ###"

static List executeJqlSearch(String jqlQuery, int startAt, int maxResults) {
    def searchRequest = Unirest.get("/rest/api/2/search")
            .queryString("jql", jqlQuery)
            .queryString("startAt", startAt)
            .queryString("maxResults", maxResults)
            .asObject(Map)
    searchRequest.status == 200 ? searchRequest.body.issues as List : null
}