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

import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Config;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.utils.GitUtils.isGitRepo;

public record WorldConfig(
        Path worldSaveDir,
        String worldUuid,
        boolean isBackupEnabled,
        boolean isShutdownBackupEnabled,
        boolean isRemoteBackupEnabled,
        String getRemotePushUrl) {

    public static final Path WORLD_UUID_PATH = Path.of("fastback/world.uuid");
    private static final String REMOTE_NAME = "origin";
    private static final String CONFIG_SECTION = "fastback";
    private static final String CONFIG_BACKUP_ENABLED = "backup-enabled";
    private static final String CONFIG_SHUTDOWN_BACKUP_ENABLED = "shutdown-backup-enabled";
    private static final String CONFIG_REMOTE_BACKUP_ENABLED = "remote-backup-enabled";

    public static WorldConfig load(Path worldSaveDir) throws IOException {
        try (Git git = Git.open(worldSaveDir.toFile())) {
            return load(worldSaveDir, git.getRepository().getConfig());
        }
    }

    public static WorldConfig load(Path worldSaveDir, Config gitConfig) throws IOException {
        return new WorldConfig(
                requireNonNull(worldSaveDir),
                getWorldUuid(worldSaveDir),
                gitConfig.getBoolean(CONFIG_SECTION, null, CONFIG_BACKUP_ENABLED, false),
                gitConfig.getBoolean(CONFIG_SECTION, null, CONFIG_SHUTDOWN_BACKUP_ENABLED, false),
                gitConfig.getBoolean(CONFIG_SECTION, null, CONFIG_REMOTE_BACKUP_ENABLED, false),
                gitConfig.getString("remote", REMOTE_NAME, "url")
        );
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

    public boolean isFileRemoteBare() {
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

    public static void setShutdownBackupEnabled(Config gitConfig, boolean value) {
        gitConfig.setBoolean(CONFIG_SECTION, null, CONFIG_SHUTDOWN_BACKUP_ENABLED, value);
    }

    public static void setRemoteBackupEnabled(Config gitConfig, boolean value) {
        gitConfig.setBoolean(CONFIG_SECTION, null, CONFIG_REMOTE_BACKUP_ENABLED, value);
    }

    public static String getWorldUuid(Path worldSaveDir) throws IOException {
        return Files.readString(worldSaveDir.resolve(WORLD_UUID_PATH)).trim();
    }

    public static void ensureWorldHasUuid(final Path worldSaveDir, final Logger logger) throws IOException {
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

    public static boolean isBackupsEnabledOn(Path worldSaveDir) {
        if (!isGitRepo(worldSaveDir)) return false;
        if (!worldSaveDir.resolve(WORLD_UUID_PATH).toFile().exists()) return false;
        return true;
    }
}


