package jira.script_console

import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.component.ComponentAccessor
import java.sql.Timestamp

/*
MutableIssue is facade for an issue's GenericValue.
After calling any 'setter' method, you will need to call store() to persist the change to the database.
Note that this is a 'shallow' store - only fields directly part of the issue (in the database schema) are persisted.
Treat this like operation similar to DB updates. Such update avoid all business JIRA logic, events, etc. Method store() is for JIRA internal usage only.
However, we can use it in order to avoid direct DB updates in cases when we understand what we do!!

 */

MutableIssue issue = ComponentAccessor.getIssueManager().getIssueObject("ID-16"); //issue key
Timestamp time = new Timestamp(2052, 3, 2, 7, 15, 22, 0);

//Timestamp(int year, int month, int date, int hour, int minute, int second, int nano)
// be careful with months(first is 0) and hours (may depend on JVM settings, etc)

issue.setCreated(time);
issue.setResolutionDate(time);
issue.setUpdated(time);

issue.store();




