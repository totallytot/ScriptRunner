import com.atlassian.crowd.embedded.impl.ImmutableUser
import com.atlassian.jira.bc.user.UserService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUsers

//Add usernames here
def users = []

def userManager = ComponentAccessor.userManager
def userService = ComponentAccessor.getComponent(UserService)
def jiraAthenticationContext = ComponentAccessor.jiraAuthenticationContext

//You have to change the value "admin" for your admin user.
def admin = userManager.getUserByName("admin")
jiraAthenticationContext.setLoggedInUser(admin)

users.each{ username ->
    def userToDisable = userManager.getUserByName(username)
    def userToDisableFromDirectory = ApplicationUsers.toDirectoryUser(userToDisable)
    def updateUser = ApplicationUsers.from(ImmutableUser.newUser(userToDisableFromDirectory).active(true).toUser())
    def updateUserValidationResult = userService.validateUpdateUser(updateUser)
    if (updateUserValidationResult.isValid()) {
        userService.updateUser(updateUserValidationResult)
    }
}
