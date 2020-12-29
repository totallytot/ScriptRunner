package jira_cloud.post_functions.gitlab

import kong.unirest.Unirest

final String gitLabApiUrl = "https://sandbox-gitlab.polontech.net/api/v4"
final int gitLabProjectId = 2
final String createBranchFrom = "master"

logger.info "Retrieving reporter email address..."
def reporterEmail = issue.fields.reporter.emailAddress
if (reporterEmail)
    logger.info "Reporter email address is ${reporterEmail}."
else {
    logger.warn "Email address is not visible. Set email address visibility to 'Anyone' in Atlassian account settings."
    return
}

logger.info "Searching for a user by email address in GitLab..."
def gitlabUserData = getUserByEmail(gitLabApiUrl, GITLAB_ADMIN_TOKEN, reporterEmail as String)
if (gitlabUserData)
    logger.info "Found GitLab user with id ${gitlabUserData.id}."
else {
    logger.warn "There is no such user with email address ${reporterEmail} in GitLab."
    return
}

logger.info "Impersonation token creation..."
def tokenData = createImpersonationToken(gitLabApiUrl, GITLAB_ADMIN_TOKEN, gitlabUserData["id"][0] as int, "jira-temporary-token")
logger.info "Impersonation token has been created with id ${tokenData.id}."

logger.info "Branch creation on behalf of the user..."
def branchName = issue.key
def branchData = createBranch(gitLabApiUrl, tokenData.token as String, gitLabProjectId, branchName as String, createBranchFrom)
logger.info "Branch has been created: ${branchData.name}."

logger.info "Revocation of the impersonation token..."
revokeImpersonationToken(gitLabApiUrl, GITLAB_ADMIN_TOKEN, gitlabUserData["id"][0] as int, tokenData.id as int)
logger.info "Successfully finished!"

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