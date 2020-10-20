package jira.post_functions.transitions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.web.bean.PagerFilter
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.workflow.TransitionOptions.Builder

String feature = (ComponentAccessor.customFieldManager.getCustomFieldObject(10201L).getValue(issue)).getKey()
List <String> statuses = ["Delivered Waiting Approval","Discontinued","Approved","Outdated"]
def applicationUser = ComponentAccessor.getUserManager().getUserByKey("auto")
List<Issue> issuesUnderFeature = new ArrayList<>()
def searchService = ComponentAccessor.getComponentOfType(SearchService.class)
def final SearchService.ParseResult parseResult = searchService.parseQuery(applicationUser, "'Parent Link' in ($feature)");
if (parseResult.isValid()) {
  def results = searchService.search(applicationUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
  issuesUnderFeature = results.getIssues()
  def value = 0
  issuesUnderFeature.each{epic->
    if (statuses.contains(epic.getStatus().name))value++
  }
  if (value==issuesUnderFeature.size()){
  	def featureIssue = ComponentAccessor.getIssueManager().getIssueObject(feature)
    def transitId = 31
    def issueService = ComponentAccessor.getIssueService()
    def builder =  new Builder()
    def transopt = builder.skipConditions().skipValidators().skipPermissions()
    def transitionValidationResult = issueService.validateTransition(applicationUser, featureIssue.getId(), transitId, issueService.newIssueInputParameters(),transopt.build())
    if (transitionValidationResult.isValid()) issueService.transition(applicationUser, transitionValidationResult)
  }  
}