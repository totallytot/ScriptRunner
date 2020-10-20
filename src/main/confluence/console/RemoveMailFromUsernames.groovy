package confluence.console

import com.atlassian.user.UserManager
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.confluence.user.UserAccessor
import com.atlassian.user.search.page.Pager

def userManager = ComponentLocator.getComponent(UserManager)
def userAccessor = ComponentLocator.getComponent(UserAccessor)

def userNames = userManager.userNames as Pager<String>
def affectedUserNames = userNames.findAll { String userName ->
    userName.contains("@")
} as List<String>

affectedUserNames.each { String userName ->
    log.warn "Old username = ${userName}"
    def newUserName = userName.substring(0, userName.indexOf("@"))
    log.warn "New username = ${newUserName}"
    userAccessor.renameUser(userAccessor.getUserByName(userName), newUserName)
}