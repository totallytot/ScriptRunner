package jira_cloud.templates

import kong.unirest.Unirest

static def getAllUsers(String gitLabApiUrl, String gitLabAdminToken) {
    Unirest.get("${gitLabApiUrl}/users")
            .header("PRIVATE-TOKEN", gitLabAdminToken)
            .asObject(List)
}

static List getUserByEmail(String gitLabApiUrl, String gitLabAdminToken, String email) {
    def result = Unirest.get("${gitLabApiUrl}/users")
            .header("PRIVATE-TOKEN", gitLabAdminToken)
            .queryString("search", email)
            .asObject(List)
    assert result.status == 200
    return result.body
}

static Map createImpersonationToken(String gitLabApiUrl, String gitLabAdminToken, int userId, String tokenName) {
    def result = Unirest.post("${gitLabApiUrl}/users/${userId}/impersonation_tokens")
            .header("PRIVATE-TOKEN", gitLabAdminToken)
            .header("Content-Type", "application/json")
            .body([
                    name : tokenName,
                    scopes: [
                            "api"
                    ]
            ]).asObject(Map)
    assert result.status == 201
    return result.body
}

static Map createBranch(String gitLabApiUrl, String gitLabToken, int gitLabProjectId, String branchName, String createBranchFrom) {
    def result = Unirest.post("${gitLabApiUrl}/projects/${gitLabProjectId}/repository/branches")
            .header("PRIVATE-TOKEN", gitLabToken)
            .header("Content-Type", "application/json")
            .body([
                    branch: branchName,
                    ref: createBranchFrom
            ]).asObject(Map)
    assert result.status == 201
    return result.body
}

static def revokeImpersonationToken(String gitLabApiUrl, String gitLabAdminToken, int userId, int tokenId) {
    def result = Unirest.delete("${gitLabApiUrl}/users/${userId}/impersonation_tokens/${tokenId}")
            .header("PRIVATE-TOKEN", gitLabAdminToken).asString()
    assert result.status == 204
}