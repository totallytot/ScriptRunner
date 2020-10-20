package jira.script_console

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;

List<String> cfs = new ArrayList<>();
for (CustomField cf : ComponentAccessor.getCustomFieldManager().getCustomFieldObjects())
{
    cfs.add("ID: " + cf.getIdAsLong() + " NAME: " + cf.getName() + " TYPE: " + cf.getCustomFieldType().getName());
}
return cfs.each{};
