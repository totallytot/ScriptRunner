package jira.conditions

import com.atlassian.jira.component.ComponentAccessor

def currentUser = ComponentAccessor.jiraAuthenticationContext.loggedInUser
def userUtil = ComponentAccessor.userUtil

if (issue.parentObject.issueType.name == "Product Requirement") {
    passesCondition = userUtil.getGroupNamesForUser(currentUser.name).any{
        it in ["product-owners", "scrum-masters"]
    }
}
