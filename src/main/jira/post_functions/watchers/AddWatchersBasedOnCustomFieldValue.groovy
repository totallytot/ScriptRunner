package jira.post_functions.watchers

import com.atlassian.jira.component.ComponentAccessor

def watcherManager = ComponentAccessor.watcherManager
def userManager = ComponentAccessor.userManager
def cfManager = ComponentAccessor.customFieldManager
def cf = cfManager.getCustomFieldObject("customfield_ID")
def value = issue.getCustomFieldValue(cf)

def watchUsers = { usernames ->
    usernames.each {
        def user = userManager.getUserByName(it.toString())
        watcherManager.startWatching(user, issue)
    }
}
if (value.toString() == "Value"){
    def users = ["username","comma","separated"]
    watchUsers(users)
}
