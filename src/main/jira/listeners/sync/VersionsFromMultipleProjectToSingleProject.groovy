package jira.listeners.sync

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.project.VersionArchiveEvent
import com.atlassian.jira.event.project.VersionCreateEvent
import com.atlassian.jira.event.project.VersionReleaseEvent
import com.atlassian.jira.event.project.VersionUnarchiveEvent
import com.atlassian.jira.event.project.VersionUnreleaseEvent
import com.atlassian.jira.event.project.VersionUpdatedEvent
import org.apache.log4j.Level
import org.apache.log4j.Logger

/**
 * Sync the versions from multiple projects to a single project.
 * Sync of:
 * - version created;
 * - version updated (name, description, start/release date);
 * - version status (released/unreleased/archived);
 */

final def destProjectKey = "TEST"
final def destProject = ComponentAccessor.projectManager.getProjectByCurrentKeyIgnoreCase(destProjectKey)
final def versionManager = ComponentAccessor.versionManager

final def logger = Logger.getLogger("VersionSync")
logger.setLevel(Level.DEBUG)

logger.debug "### Version Sync Started ###"
switch (event.class.simpleName) {
    case "VersionCreateEvent":
        logger.debug "VersionCreateEvent"
        final def sourceVersion = (event as VersionCreateEvent).version
        versionManager.createVersion(sourceVersion.name, sourceVersion.startDate, sourceVersion.releaseDate, sourceVersion.description,
                destProject.id, null, sourceVersion.released)
        break
    case "VersionUpdatedEvent":
        logger.debug "VersionUpdatedEvent"
        final def sourceOriginalVersion = (event as VersionUpdatedEvent).originalVersion
        final def sourceVersion = (event as VersionUpdatedEvent).version
        def destVersion = versionManager.getVersions(destProject).find { it.name == sourceOriginalVersion.name }
        if (!destVersion)
            destVersion = versionManager.createVersion(sourceVersion.name, sourceVersion.startDate, sourceVersion.releaseDate,
                    sourceVersion.description, destProject.id, null, sourceVersion.released)
        if (sourceVersion.name != destVersion.name || sourceVersion.description != destVersion.description)
            destVersion = versionManager.editVersionDetails(destVersion, sourceVersion.name, sourceVersion.description)
        if (sourceVersion.startDate != destVersion.startDate || sourceVersion.releaseDate != destVersion.releaseDate)
            versionManager.editVersionStartReleaseDate(destVersion, sourceVersion.startDate, sourceVersion.releaseDate)
        break
    case "VersionArchiveEvent":
        logger.debug "VersionArchiveEvent"
        final def sourceVersion = (event as VersionArchiveEvent).version
        def destVersion = versionManager.getVersions(destProject).find { it.name == sourceVersion.name }
        if (!destVersion)
            destVersion = versionManager.createVersion(sourceVersion.name, sourceVersion.startDate, sourceVersion.releaseDate,
                    sourceVersion.description, destProject.id, null, sourceVersion.released)
        versionManager.archiveVersion(destVersion, true)
        break
    case "VersionUnarchiveEvent":
        logger.debug "VersionUnarchiveEvent"
        final def sourceVersion = (event as VersionUnarchiveEvent).version
        def destVersion = versionManager.getVersions(destProject).find { it.name == sourceVersion.name }
        if (!destVersion)
            destVersion = versionManager.createVersion(sourceVersion.name, sourceVersion.startDate, sourceVersion.releaseDate,
                    sourceVersion.description, destProject.id, null, sourceVersion.released)
        versionManager.archiveVersion(destVersion, false)
        break
    case "VersionReleaseEvent":
        logger.debug "VersionReleaseEvent"
        final def sourceVersion = (event as VersionReleaseEvent).version
        def destVersion = versionManager.getVersions(destProject).find { it.name == sourceVersion.name }
        if (!destVersion)
            destVersion = versionManager.createVersion(sourceVersion.name, sourceVersion.startDate, sourceVersion.releaseDate,
                    sourceVersion.description, destProject.id, null, sourceVersion.released)
        versionManager.releaseVersion(destVersion,true)
        if (sourceVersion.releaseDate != destVersion.releaseDate) versionManager.editVersionReleaseDate(destVersion, sourceVersion.releaseDate)
        break
    case "VersionUnreleaseEvent":
        logger.debug "VersionUnreleaseEvent"
        final def sourceVersion = (event as VersionUnreleaseEvent).version
        def destVersion = versionManager.getVersions(destProject).find { it.name == sourceVersion.name }
        if (!destVersion)
            destVersion = versionManager.createVersion(sourceVersion.name, sourceVersion.startDate, sourceVersion.releaseDate,
                    sourceVersion.description, destProject.id, null, sourceVersion.released)
        versionManager.releaseVersion(destVersion, false)
        break
    default:
        break
}
logger.debug "### Version Sync Ended ###"