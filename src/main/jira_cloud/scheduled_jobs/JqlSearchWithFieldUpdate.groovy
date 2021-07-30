package jira_cloud.scheduled_jobs

import kong.unirest.Unirest

import java.text.NumberFormat

logger.info "### Calculated Rank Scheduled Job Started ###"
def calculatedRankCfId = "customfield_12727"
def requestCountCfId = "customfield_12654"
def jqlQuery = 'project = "Connect" and status != "Closed"'
int startAt, jqlOutputItemsCount
def maxResults = 100
def searchResult = []

// workaround for old Groovy version w/t do-while
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
    if (!votes) votes = 0
    def requestCountVal = issue.fields["${requestCountCfId}"] as Double
    if (!requestCountVal) requestCountVal = 0
    def calculatedRankVal = (requestCountVal * 0.4 + votes * 0.6) as Double
    logger.info "Actual calculatedRank value is ${calculatedRankVal}"

    // formatting
    def numberFormat = NumberFormat.instance
    numberFormat.setMaximumFractionDigits(3)
    numberFormat.setMinimumFractionDigits(1)
    def calculatedRankValFormatted = Double.parseDouble(numberFormat.format(calculatedRankVal))
    logger.info "Formatted Actual calculatedRank value is ${calculatedRankValFormatted}"

    def currentCalculatedRankVal = issue.fields[calculatedRankCfId] as Double
    logger.info "Current calculatedRank value is ${currentCalculatedRankVal}"
    def isUpdateRequired = (calculatedRankValFormatted != currentCalculatedRankVal)
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