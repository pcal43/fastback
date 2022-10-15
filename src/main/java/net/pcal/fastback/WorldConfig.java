/*
 * FastBack - Fast, incremental Minecraft backups powered by Git.
 * Copyright (C) 2022 pcal.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package net.pcal.fastback;

import net.pcal.fastback.commands.SchedulableAction;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static net.pcal.fastback.utils.FileUtils.writeResourceToFile;

public record WorldConfig(
        String worldUuid,
        boolean isBackupEnabled,
        SchedulableAction autobackAction,
        Duration autobackWait,
        SchedulableAction shutdownAction,
        String retentionPolicy,
        String getRemotePushUrl) {

    public static final Path WORLD_UUID_PATH = Path.of("fastback/world.uuid");
    private static final String REMOTE_NAME = "origin";
    private static final String CONFIG_SECTION = "fastback";
    private static final String CONFIG_BACKUP_ENABLED = "backup-enabled";
    private static final String CONFIG_RETENTION_POLICY = "retention-policy";
    private static final String CONFIG_AUTOBACK_ACTION = "autoback-action";
    private static final String CONFIG_AUTOBACK_WAIT = "autoback-wait";
    private static final String CONFIG_SHUTDOWN_ACTION = "shutdown-action";
    private static final String CONFIG_UPDATE_GITIGNORE_ENABLED = "update-gitignore-enabled";
    private static final String CONFIG_UPDATE_GITATTRIBUTES_ENABLED = "update-gitattributes-enabled";

    public static WorldConfig load(final Git git) throws IOException {
        final StoredConfig gitConfig = git.getRepository().getConfig();
        final SchedulableAction autobackAction = retrieveAction(gitConfig, CONFIG_AUTOBACK_ACTION);
        final int autobackWait = gitConfig.getInt(CONFIG_SECTION, CONFIG_AUTOBACK_WAIT, 0);
        /*final*/ SchedulableAction shutdownAction = retrieveAction(gitConfig, CONFIG_SHUTDOWN_ACTION);
        if (shutdownAction == null) {
            // provide backward compat for 0.1.x configs.  TODO remove this
            if (gitConfig.getBoolean(CONFIG_SECTION, null, "shutdown-backup-enabled", false)) {
                shutdownAction = SchedulableAction.DEFAULT_SHUTDOWN_ACTION;
            }
        }
        return new WorldConfig(
                getWorldUuid(git),
                gitConfig.getBoolean(CONFIG_SECTION, null, CONFIG_BACKUP_ENABLED, false),
                autobackAction,
                Duration.ofMinutes(autobackWait),
                shutdownAction,
                gitConfig.getString(CONFIG_SECTION, null, CONFIG_RETENTION_POLICY),
                gitConfig.getString("remote", REMOTE_NAME, "url")
        );
    }

    private static SchedulableAction retrieveAction(Config gitConfig, String configKey) {
        final String shutdownActionRaw = gitConfig.getString(CONFIG_SECTION, null, configKey);
        return shutdownActionRaw != null ? SchedulableAction.getForConfigKey(shutdownActionRaw) : null;
    }

    // THESE ARE EFFECTIVELY CONSTANTS.  HERE BECAUSE WE MIGHT NEED TO MAKE SOME OF THEM CONFIGURABLE SOMEDAY.

    public String getRemoteName() {
        return REMOTE_NAME;
    }

    public boolean isUuidCheckEnabled() {
        return true;
    }

    public boolean isTempBranchCleanupEnabled() {
        return true;
    }

    public boolean isRemoteTempBranchCleanupEnabled() {
        return true;
    }

    public boolean isSmartPushEnabled() {
        return true;
    }

    public boolean isPostRestoreCleanupEnabled() {
        return true;
    }

    // REMEMBER TO CALL config.save() YOURSELF!!

    public static void setRemoteUrl(Config gitConfig, String url) {
        gitConfig.setString("remote", REMOTE_NAME, "url", url);
    }

    public static void setBackupEnabled(Config gitConfig, boolean value) {
        gitConfig.setBoolean(CONFIG_SECTION, null, CONFIG_BACKUP_ENABLED, value);
    }

    public static void setRetentionPolicy(Config gitConfig, String value) {
        gitConfig.setString(CONFIG_SECTION, null, CONFIG_RETENTION_POLICY, value);
    }

    public static void setAutobackAction(Config gitConfig, SchedulableAction action) {
        gitConfig.setString(CONFIG_SECTION, null, CONFIG_AUTOBACK_ACTION, action.getConfigKey());
    }

    public static void setAutobackWait(Config gitConfig, int waitTimeMinutes) {
        gitConfig.setInt(CONFIG_SECTION, null, CONFIG_AUTOBACK_WAIT, waitTimeMinutes);
    }

    public static void setShutdownAction(Config gitConfig, SchedulableAction action) {
        gitConfig.setString(CONFIG_SECTION, null, CONFIG_SHUTDOWN_ACTION, action.getConfigKey());
    }

    public static String getWorldUuid(Git git) throws IOException {
        return Files.readString(git.getRepository().getWorkTree().toPath().
                toAbsolutePath().resolve(WORLD_UUID_PATH)).trim();
    }

    private static void ensureWorldHasUuid(final Path worldSaveDir, final Logger logger) throws IOException {
        final Path worldUuidpath = worldSaveDir.resolve(WORLD_UUID_PATH);
        if (!worldUuidpath.toFile().exists()) {
            FileUtils.mkdirs(worldUuidpath.getParent());
            final String newUuid = UUID.randomUUID().toString();
            try (final FileWriter fw = new FileWriter(worldUuidpath.toFile())) {
                fw.append(newUuid);
                fw.append('\n');
            }
            logger.info("Generated new world.uuid " + newUuid);
        }
    }


    // ====================================================================
    // Resource management

    private record WorldResource(
            Path resourcePath,
            Path targetPath,
            String permission
    ) {}

    private static final Iterable<WorldResource> WORLD_RESOURCES = List.of(
            new WorldResource(
                    Path.of("world/dot-gitignore"),
                    Path.of(".gitignore"),
                    CONFIG_UPDATE_GITIGNORE_ENABLED),
            new WorldResource(
                    Path.of("world/dot-gitattributes"),
                    Path.of(".gitattributes"),
                    CONFIG_UPDATE_GITATTRIBUTES_ENABLED)
    );

    public static void doWorldMaintenance(final Git git, final Logger logger) throws IOException {
        logger.info("Doing world maintenance");
        final Path worldSaveDir = git.getRepository().getWorkTree().toPath();
        ensureWorldHasUuid(worldSaveDir, logger);
        final Config config = git.getRepository().getConfig();
        for (final WorldResource resource : WORLD_RESOURCES) {
            if (config.getBoolean(CONFIG_SECTION, resource.permission, true)) {
                logger.debug("Updating "+resource.targetPath);
                final Path targetPath = worldSaveDir.resolve(resource.targetPath);
                writeResourceToFile(resource.resourcePath, targetPath);
            } else {
                logger.info("Updates disabled for "+resource.targetPath);
            }
        }
    }
}
