package jira.conditions

import jira.JiraUtilHelper

def insightCfVal = JiraUtilHelper.getCustomFieldValue("Подразделение", issue) as String
if (!insightCfVal) return
return !(insightCfVal == "[Технологический Центр Нудоль (ITSM-41)]" && issue.priority.name in ["Высокий"])