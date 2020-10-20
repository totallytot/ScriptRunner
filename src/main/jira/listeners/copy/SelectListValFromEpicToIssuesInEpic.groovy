package jira.listeners.copy

import JiraUtilHelper

def a = new JiraUtilHelper()
if (issue.issueType.name != "Epic") return
