package jira.jobs

import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.issue.label.LabelManager

/**
 * In the board configuration we have option to hide the completed issues older than 1 week or 2 weeks or 4 weeks or show all.
 * So we select show all option. The board displays the issue according the the configured filter in the board.
 * So if we want the issues to be not displayed at some point of time we need the issue to become outside the filter.
 * It can be achieved by adding label 'archived_hide_from_tcc_board'. The original filter will be changed to:
 * Original filter AND label != label 'archived_hide_from_tcc_board
 * When the epic is completed we can setup the scheduler which will add label 'archived_hide_from_tcc_board' to
 * completed epic and in it's tasks(issue in the epic) after X days. For X days - the initial value is 2 weeks.
 */

def username = "service_account"
def executionUser = ComponentAccessor.userManager.getUserByName(username)
def resolutionPeriodDays = 1
def archLabel = "archived_hide_from_tcc_board"
// find all epicsWithoutIssues that were resolved more than x days ago
def jql = "project=RAD and issuetype=Epic and (labels is empty or labels!=${archLabel}) and resolved<=-${resolutionPeriodDays}d"

static List<Issue> getIssuesFromJql(ApplicationUser executionUser, String jql) {
    def searchService = ComponentAccessor.getComponentOfType(SearchService)
    def parseResult = searchService.parseQuery(executionUser, jql)
    if (parseResult.valid) searchService.search(executionUser, parseResult.query, PagerFilter.unlimitedFilter).results
    else null
}

static updateIssueLabels(ApplicationUser executionUser, String label, Issue issue) {
    def labelManager = ComponentAccessor.getComponentOfType(LabelManager)
    def existingLabels = labelManager.getLabels(issue.id)*.label
    def labelsToSet = (existingLabels + label).toSet()
    def sendNotification = false
    def issueUpdateEventAndReindex = true
    labelManager.setLabels(executionUser, issue.id, labelsToSet, sendNotification, issueUpdateEventAndReindex)
}

static List<Issue> getIssuesInEpic(Issue epic) {
    ComponentAccessor.issueLinkManager.getOutwardLinks(epic.id).
            findAll { it.issueLinkType.name == "Epic-Story Link" }*.destinationObject
}

getIssuesFromJql(executionUser, jql).each { epic ->
    updateIssueLabels(executionUser, archLabel, epic as Issue)
    getIssuesInEpic(epic as Issue).each { updateIssueLabels(executionUser, archLabel, it) }
}