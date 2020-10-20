package jira.post_functions

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.label.Label

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def cf1 = customFieldManager.getCustomFieldObject(28001L) //Visa
def cf2 = customFieldManager.getCustomFieldObject(28002L) //City of Application

String visa = issue.getCustomFieldValue(cf1)
String source2 = null

HashSet<Label> labels = (Set<Label>) issue.getCustomFieldValue(cf2)

if (issue.getIssueType().getName().equals("Visa Request")) {
    for (Label label : labels)
    {
        source2 = label.getLabel()
    }
    switch (visa) {
        case "United States":
            if (source2.equalsIgnoreCase("Minsk"))
            {
                issue.setOriginalEstimate(3600*2L)
                issue.setEstimate(3600*2L)
                break
            }
            else if (source2.equalsIgnoreCase("Kiev") || source2.equalsIgnoreCase("Moscow") || source2.equalsIgnoreCase("Yerevan"))
            {
                issue.setOriginalEstimate(16200L)
                issue.setEstimate(16200L)
                break
            }
            break
        case "Belarus":
        case "Lithuania":
            issue.setOriginalEstimate(1800L) //0.5h
            issue.setEstimate(1800L)
            break
        case "United Arab Emirates":
            issue.setOriginalEstimate(3600L) //1h
            issue.setEstimate(3600L)
            break
        case "Germany":
        case "Hungary":
            issue.setOriginalEstimate(3600*2L)
            issue.setEstimate(3600*2L)
            break
        case "Austria":
        case "Belgium":
        case "Bulgaria":
        case "China":
        case "Czech Republic":
        case "Denmark":
        case "France":
        case "Ireland":
        case "Italy":
        case "Netherlands":
        case "Norway":
        case "Poland":
        case "Singapore":
        case "Spain":
        case "Sweden":
        case "Switzerland":
            issue.setOriginalEstimate(3600*3L)
            issue.setEstimate(3600*3L)
            break
        case "India":
            issue.setOriginalEstimate(12600L) //3.5h
            issue.setEstimate(12600L)
            break
        case "Australia":
        case "Canada":
        case "United Kingdom":
            issue.setOriginalEstimate(3600*5L)
            issue.setEstimate(3600*5L)
            break
    }
}
