package jira.script_console

import com.atlassian.crowd.embedded.impl.ImmutableUser
import com.atlassian.jira.user.ApplicationUsers
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.DelegatingApplicationUser

//can be used for bulk user renaming in JIRA internal dir

def userManager = ComponentAccessor.userManager
def users = userManager.users

users.each { ApplicationUser user ->
    def email = user.emailAddress
    if (email) {
        def newName = email.substring(0, email.indexOf("@")).toLowerCase()
        if (newName != user.name.toLowerCase()) {
            log.warn "New name: ${newName}"
            log.warn "Old name: ${user.name}"
            ImmutableUser.Builder builder = ImmutableUser.newUser(ApplicationUsers.toDirectoryUser(user))
            builder.name(newName)
            builder.toUser()
            userManager.updateUser(new DelegatingApplicationUser(user.id, user.key, builder.toUser()))
        }
    }
}