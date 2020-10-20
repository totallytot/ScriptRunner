package jira.script_console

import com.atlassian.crowd.embedded.impl.ImmutableUser
import com.atlassian.jira.user.ApplicationUsers
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.user.util.UserUtil
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.DelegatingApplicationUser

//can be used for bulk user renaming in JIRA internal dir
UserUtil userUtil = ComponentAccessor.getUserUtil()
List<ApplicationUser> users = (List<ApplicationUser>) userUtil.getUsers()

for (ApplicationUser user : users)
{
    if (user.getUsername().contains("@some.com")) {
        ImmutableUser.Builder builder = ImmutableUser.newUser(ApplicationUsers.toDirectoryUser(user))
        builder.name(user.getUsername().substring(0, user.getUsername().length() - 9))
        builder.toUser()
        UserManager userManager = ComponentAccessor.getUserManager()
        userManager.updateUser(new DelegatingApplicationUser(user.getId(), user.getKey(), builder.toUser()))
    }
}


