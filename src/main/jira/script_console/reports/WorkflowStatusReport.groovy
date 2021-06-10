package jira.script_console.reports

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.status.Status;
List<String> statuses = new ArrayList<>();
for (Status status : ComponentAccessor.getConstantsManager().getStatuses())
{
    statuses.add("ID: " + status.getId() + " NAME: " + status.getName() + " CATEGORY: " + status.getStatusCategory().getName() + " COLOR: " + status.getStatusCategory().getColorName());
}
return statuses.each{};