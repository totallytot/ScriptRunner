package jira.script_console

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.issue.search.SearchResults
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.web.bean.PagerFilter

String jqlQuery = "project = Batches AND issuetype = Batch ORDER BY summary ASC";
def applicationUser = ComponentAccessor.getUserManager().getUserByKey("auto");
def searchService = ComponentAccessor.getComponentOfType(SearchService.class);
List<Issue> issues = new ArrayList<>();
List<Issue> issuesForUpdate = new ArrayList<>();
List<Issue> batchesForUpdate = new ArrayList<>();
List<Issue> issuesForDeletion = new ArrayList<>();
final SearchService.ParseResult parseResult = searchService.parseQuery(applicationUser, jqlQuery);
if (parseResult.isValid()) {
    final SearchResults results = searchService.search(applicationUser, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
    issues = results.getIssues()
}
issues.sort()
for (def i=0;i<=issues.size()-1;i++){
    if (!issuesForDeletion.contains(issues[i])){
        batchesForUpdate = issues?.findAll(){it.getSummary() == issues[i].getSummary() && it!=issues[i] && !issuesForDeletion.contains(it)}
        String batchesForUpdateKeys = ""
    	batchesForUpdate.each{
            issuesForDeletion.add(it)
            batchesForUpdateKeys += it.getKey() +','    
        }
        if (batchesForUpdateKeys!="") {
            batchesForUpdateKeys= batchesForUpdateKeys.substring(0,batchesForUpdateKeys.length()-1)
            jqlQuery = "'Parent Link' in ($batchesForUpdateKeys)"
            final SearchService.ParseResult parseResult2 = searchService.parseQuery(applicationUser, jqlQuery);
		    if (parseResult.isValid()) {
    			final SearchResults results = searchService.search(applicationUser, parseResult2.getQuery(), PagerFilter.getUnlimitedFilter())
    			issuesForUpdate = results.getIssues()                 
                if (issuesForUpdate!=null && issuesForUpdate.size()>0){
        	        for (def y=0;y<=issuesForUpdate.size()-1;y++){
           		        def issueForUpdate = issuesForUpdate[y]
           		        def customFieldManager = ComponentAccessor.getCustomFieldManager()
		   		        def parentLink = customFieldManager.getCustomFieldObjectByName('Parent Link')
           		        def issueService = ComponentAccessor.getIssueService()
        		        def issueInputParameters = issueService.newIssueInputParameters()
        		        issueInputParameters.addCustomFieldValue(parentLink.getIdAsLong(), issues[i].getKey())
        		        IssueService.UpdateValidationResult validationResult = issueService.validateUpdate(applicationUser, issueForUpdate.getId(), issueInputParameters)            
            	        if (validationResult.isValid()) issueService.update(applicationUser, validationResult)
          	        }
                }
            }
        }
    }
}
List<String> forsearch = new ArrayList<>()
issuesForDeletion.each{  forsearch.add(it.getKey())}
return forsearch