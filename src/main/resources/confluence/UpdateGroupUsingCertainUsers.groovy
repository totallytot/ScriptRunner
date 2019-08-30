package confluence

import com.atlassian.confluence.user.UserAccessor
import com.atlassian.spring.container.ContainerManager

def userAccessor = ContainerManager.getInstance().getComponent("userAccessor") as UserAccessor
def allUserNames = userAccessor.userNamesWithConfluenceAccess
def group = userAccessor.getGroup("internal-users")
allUserNames.each { userName ->
    def user = userAccessor.getUserByName(userName)
    if (user.email.contains("@example.com")) {
        userAccessor.addMembership(group, user)
    }
}