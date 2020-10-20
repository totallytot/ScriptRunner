package confluence.console

import com.atlassian.confluence.internal.security.SpacePermissionContext
import com.atlassian.confluence.internal.security.SpacePermissionManagerInternal
import com.atlassian.confluence.security.SpacePermission
import com.atlassian.confluence.spaces.Space
import com.atlassian.spring.container.ContainerManager
import com.atlassian.confluence.spaces.SpaceManager

def manager = ContainerManager.instance.getComponent("spacePermissionManager") as SpacePermissionManagerInternal
SpaceManager sm = (SpaceManager) ContainerManager.getInstance().getComponent("spaceManager")

List<Space> all = sm.getAllSpaces()
Set<String> groups = new HashSet<>()
groups.add("confluence-administrators")
groups.add("confluence-users")

for (Space space : all) {
    List<SpacePermission> spacePermissionsToRemove = new ArrayList<>(space.getPermissions())
    for (SpacePermission spacePermission : spacePermissionsToRemove) {
        if (spacePermission.isGroupPermission() && groups.contains(spacePermission.getGroup()))
            manager.removePermission(spacePermission, SpacePermissionContext.createDefault())
    }
}