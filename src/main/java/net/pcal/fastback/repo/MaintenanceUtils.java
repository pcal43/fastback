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

package net.pcal.fastback.repo;

import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.SystemLogger;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.utils.EnvironmentUtils;
import net.pcal.fastback.utils.FileUtils;
import org.eclipse.jgit.api.Git;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;

import static net.pcal.fastback.config.GitConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.UPDATE_GITATTRIBUTES_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.UPDATE_GITIGNORE_ENABLED;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.NATIVE_GIT;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.WARNING;
import static net.pcal.fastback.logging.UserMessage.localized;
import static net.pcal.fastback.logging.UserMessage.raw;
import static net.pcal.fastback.logging.UserMessage.styledRaw;
import static net.pcal.fastback.repo.RepoImpl.WORLD_UUID_PATH;
import static net.pcal.fastback.utils.FileUtils.writeResourceToFile;
import static net.pcal.fastback.utils.ProcessUtils.doExec;

/**
 * Utilities for keeping the repo configuration up-to-date.
 *
 * @author pcal
 * @since 0.13.0
 */
public interface MaintenanceUtils {

    // ======================================================================
    // Util methods

    /**
     * Should be called prior to any heavy-lifting with git (e.g. commiting and pushing).  Ensures that
     * key files are all set correctly.
     */
    static void doPreflight(RepoImpl repo) throws IOException {
        final SystemLogger syslog = syslog();
        syslog.debug("Doing world maintenance");
        final Git jgit = repo.getJGit();
        final Path worldSaveDir = jgit.getRepository().getWorkTree().toPath();
        ensureWorldHasUuid(worldSaveDir);
        final GitConfig config = GitConfig.load(jgit);
        if (config.getBoolean(UPDATE_GITIGNORE_ENABLED)) {
            final Path targetPath = worldSaveDir.resolve(".gitignore");
            writeResourceToFile("world/gitignore", targetPath);
        }
        if (config.getBoolean(UPDATE_GITATTRIBUTES_ENABLED)) {
            final Path targetPath = worldSaveDir.resolve(".gitattributes");
            if (config.getBoolean(IS_NATIVE_GIT_ENABLED)) {
                writeResourceToFile("world/gitattributes-native", targetPath);
            } else {
                writeResourceToFile("world/gitattributes-jgit", targetPath);
            }
        }
        try {
            ensureNativeLfsInstallation(repo);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    /**
     * FIXME i18n
     */
    static void setNativeGitEnabled(boolean newSetting, final Repo repo, final UserLogger user) throws IOException {
        final GitConfig conf = repo.getConfig();
        boolean currentSetting = repo.getConfig().getBoolean(IS_NATIVE_GIT_ENABLED);
        if (currentSetting == newSetting) {
            user.message(raw("Nothing changed."));
            return;
        }
        if (!repo.listSnapshots().isEmpty()) {
            user.message(styledRaw("Existing snapshots found.  You can't change the native-git setting after you've " +
                    "done a backup.  Consider making a fresh copy of your world, deleting the .git directory " +
                    "in the copy, and enabling native git there.", ERROR));
            return;
        }
        if (newSetting) {
            if (!EnvironmentUtils.isNativeGitInstalled()) {
                user.message(styledRaw("Please install git and git-lfs and try again.", ERROR));
                return;
            } else {
            }
            conf.updater().set(IS_NATIVE_GIT_ENABLED, true).save();
            user.message(localized("fastback.chat.ok"));
            user.message(styledRaw("native-git enabled.", NATIVE_GIT));
            user.message(styledRaw("WARNING!  This is an experimental feature.  Please don't use it on a world you love.", WARNING));
        } else {
            conf.updater().set(IS_NATIVE_GIT_ENABLED, false).save();
            user.message(localized("fastback.chat.ok"));
        }
    }

    static void createWorldUuid(final Path worldSaveDir) throws IOException {
        final Path worldUuidpath = worldSaveDir.resolve(WORLD_UUID_PATH).toAbsolutePath().normalize();
        FileUtils.mkdirs(worldUuidpath.getParent());
        final String newUuid = UUID.randomUUID().toString();
        try (final FileWriter fw = new FileWriter(worldUuidpath.toFile())) {
            fw.append(newUuid);
            fw.append('\n');
        }
        syslog().debug("Generated new world.uuid " + newUuid);
    }

    static void ensureWorldHasUuid(final Path worldSaveDir) throws IOException {
        final Path worldUuidpath = worldSaveDir.resolve(WORLD_UUID_PATH).toAbsolutePath().normalize();
        if (!worldUuidpath.toFile().exists()) {
            syslog().warn("Did not find expected uuid file at "+worldUuidpath);
            syslog().warn("We'll create a new one and carry on.  But this indicates something weird is going on.");
            createWorldUuid(worldSaveDir);
        }
    }

    // ======================================================================
    // Private


    /**
     * Ensures that git-lfs is installed or uninstalled in the worktree as appropriate.
     */
    private static void ensureNativeLfsInstallation(final Repo repo) throws IOException, InterruptedException {
        if (EnvironmentUtils.isNativeGitInstalled()) {
            final boolean isNativeEnabled = repo.getConfig().getBoolean(IS_NATIVE_GIT_ENABLED);
            final String action = isNativeEnabled ? "install" : "uninstall";
            final String[] cmd = {"git", "-C", repo.getWorkTree().getAbsolutePath(), "lfs", action, "--local"};
            doExec(cmd, Collections.emptyMap(), s -> {
            }, s -> {
            });
        }
    }
}
