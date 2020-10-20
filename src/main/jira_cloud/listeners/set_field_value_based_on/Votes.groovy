package jira_cloud.listeners.set_field_value_based_on

import kong.unirest.Unirest

if (!changelog.toString().contains("Requests Count")) return

def requestVotes = Unirest.get("/rest/api/2/issue/${issue.key}/votes").asObject(Map)
assert requestVotes.status == 200
def votes = requestVotes.body.votes as Integer

def requestCF = Unirest.get("/rest/api/2/field").asObject(List)
assert requestCF.status == 200
def cfs = requestCF.body.findAll { (it as Map).custom } as List<Map>

def calcRankId = cfs.find { it.name == "Calculated Rank" }?.id
assert calcRankId != null

def requestCountId = cfs.find { it.name == "Requests Count" }?.id
assert requestCountId != null

def requestCountVal = issue.fields["${requestCountId}"] as Integer
assert requestCountVal != null

def calcRankVal = requestCountVal * 0.4 + votes * 0.6

def result = Unirest.put("/rest/api/2/issue/${issue.key}")
        .queryString("overrideScreenSecurity", Boolean.TRUE)
        .queryString("notifyUsers", Boolean.FALSE)
        .header("Content-Type", "application/json")
        .body([
                fields: [
                        (calcRankId): calcRankVal
                ]
        ]).asString()
assert result.status == 204