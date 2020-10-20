package confluence.console

import com.atlassian.confluence.pages.Page
import com.atlassian.confluence.pages.PageManager
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.spring.container.ContainerManager

def pageManager = (PageManager) ContainerManager.getInstance().getComponent("pageManager")
def spaceManager = (SpaceManager) ContainerManager.getInstance().getComponent("spaceManager")

def space = spaceManager.getSpace("SPACEKEY")
def pages = pageManager.getPages(space, true) //boolean currentOnly

def versionsToLeave = 0
def removedPagesTitles = []

pages.each {
    int previousVersion = it.getPreviousVersion()
    if (previousVersion > versionsToLeave) {
        for (int i = 1; i <= (previousVersion - versionsToLeave); i++) {
            try {
                def pageForRemoval = (Page) pageManager.getPageByVersion(it, i)
                removedPagesTitles << "title: " + pageForRemoval.displayTitle + " - version: " + pageForRemoval.version
                pageForRemoval.remove(pageManager)
            } catch (NullPointerException e) {
                log.error(e.message)
            }
        }
    }
}
removedPagesTitles.size()