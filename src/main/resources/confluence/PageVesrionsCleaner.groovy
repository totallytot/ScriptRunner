package confluence

import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.spring.container.ContainerManager;

PageManager pageManager = (PageManager) ContainerManager.getInstance().getComponent("pageManager");
SpaceManager spaceManager = (SpaceManager) ContainerManager.getInstance().getComponent("spaceManager");

Space space = spaceManager.getSpace("EPMBDASFC");
List<Page> pages = pageManager.getPages(space, true); //boolean currentOnly

int versionsToLeave = 3;

for (Page page : pages) {
    int previousVersion = page.getPreviousVersion();

    if (previousVersion > versionsToLeave) {

        for (int i = 1; i <= (previousVersion - versionsToLeave); i++) {

            try {
                Page pageForRemoval = (Page) pageManager.getPageByVersion(page, i);
                pageForRemoval.remove(pageManager);
            } catch (NullPointerException e) {
                //do nothing
            }
        }
    }
}
