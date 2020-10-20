package jira.scripted_fields

import com.atlassian.jira.component.ComponentAccessor

//Script Location: Scripted Field
//Searcher: Number Searcher
//Template: Number Field

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def storyPoints = customFieldManager.getCustomFieldObject(10307L)
def businessValue = customFieldManager.getCustomFieldObject(10300L)

String sp = issue.getCustomFieldValue(storyPoints)
String bv = issue.getCustomFieldValue(businessValue)

if (sp == null || sp.isEmpty()) sp = "0"
def spValue = Double.parseDouble(sp)

if (bv == null || bv.isEmpty()) return null
def bvValue = Double.parseDouble(bv)

if (spValue >= 0 && spValue <= 3 && bvValue == 3) return 1
else if (spValue >= 0 && spValue <= 3 && bvValue >= 1 && bvValue <= 2) return 3
else if (spValue > 3 && bvValue == 3) return 2
else if (spValue > 3 && bvValue >= 1 && bvValue <= 2) return 4
else return null