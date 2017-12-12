//Put to Administration -> Ad-dons -> Script Console.

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.issuetype.IssueType;
List<String> issueTypes = new ArrayList<>();
for (IssueType issueType : ComponentAccessor.getConstantsManager().getAllIssueTypeObjects())
{
    issueTypes.add("ID: " + issueType.getId() + " NAME: " + issueType.getName());
}
return issueTypes.each{};