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
import net.pcal.fastback.utils.EnvironmentUtils;
import net.pcal.fastback.utils.ProcessException;
import org.eclipse.jgit.api.Git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import static net.pcal.fastback.config.FastbackConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.UPDATE_GITATTRIBUTES_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.UPDATE_GITIGNORE_ENABLED;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.utils.FileUtils.writeResourceToFile;
import static net.pcal.fastback.utils.ProcessUtils.doExec;

/**
 * Utilities for keeping the repo configuration up-to-date.
 *
 * @author pcal
 * @since 0.13.0
 */
abstract class PreflightUtils {

    // ======================================================================
    // Util methods

    /**
     * Should be called prior to any heavy-lifting with git (e.g. commiting and pushing).  Ensures that
     * key files are all set correctly.
     */
    static void doPreflight(RepoImpl repo) throws IOException, ProcessException {
        final SystemLogger syslog = syslog();
        syslog.debug("Doing world maintenance");
        final Git jgit = repo.getJGit();
        final Path worldSaveDir = jgit.getRepository().getWorkTree().toPath();
        WorldIdUtils.ensureWorldHasId(worldSaveDir);
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
        updateNativeLfsInstallation(repo);
    }

    // ======================================================================
    // Private

    /**
     * Ensures that git-lfs is installed or uninstalled in the worktree as appropriate.
     */
    private static void updateNativeLfsInstallation(final Repo repo) throws ProcessException {
        if (EnvironmentUtils.isNativeGitInstalled()) {
            final boolean isNativeEnabled = repo.getConfig().getBoolean(IS_NATIVE_GIT_ENABLED);
            final String action = isNativeEnabled ? "install" : "uninstall";
            final String[] cmd = {"git", "-C", repo.getWorkTree().getAbsolutePath(), "lfs", action, "--local"};
            doExec(cmd, Collections.emptyMap(), s -> {}, s -> {});
        }
    }
}
