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

import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.repo.RepoImpl.WORLD_UUID_PATH;

/**
 * Utilities for restoring a snapshot
 *
 * @author pcal
 */
class RestoreUtils {

    // ======================================================================
    // Utility methods

    static Path restoreSnapshot(final String repoUri, final Path restoreTargetDir, final String worldName, final SnapshotId sid, final Logger logger) throws IOException {
        try {
            return jgit_restoreSnapshot(repoUri, restoreTargetDir, worldName, sid, logger);
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }

    // ======================================================================
    // Private

    private static Path jgit_restoreSnapshot(final String repoUri, final Path restoreTargetDir, final String worldName, final SnapshotId sid, final Logger logger) throws IOException, GitAPIException {
        final Path restoreDir = getTargetDir(restoreTargetDir, worldName, sid.getName());
        final String branchName = sid.getBranchName();
        logger.hud(localized("fastback.hud.restore-percent", 0));
        final ProgressMonitor pm = new JGitIncrementalProgressMonitor(new JGitRestoreProgressMonitor(logger), 100);
        try (Git git = Git.cloneRepository().setProgressMonitor(pm).setDirectory(restoreDir.toFile()).
                setBranchesToClone(List.of("refs/heads/" + branchName)).setBranch(branchName).setURI(repoUri).call()) {
        }
        FileUtils.rmdir(restoreDir.resolve(".git"));
        restoreDir.resolve(WORLD_UUID_PATH).toFile().delete();
        return restoreDir;
    }

    private static Path getTargetDir(Path saveDir, String worldName, String snapshotName) {
        worldName = worldName.replaceAll("\\W+", ""); // strip out all non-word characters for safety
        Path base = saveDir.resolve(worldName + "-" + snapshotName);
        Path candidate = base;
        int i = 0;
        while (candidate.toFile().exists()) {
            i++;
            candidate = Path.of(base + "_" + i);
            if (i > 1000) {
                throw new IllegalStateException("wat i = " + i);
            }
        }
        return candidate;
    }

    private static class JGitRestoreProgressMonitor extends JGitPercentageProgressMonitor {

        private final Logger logger;

        public JGitRestoreProgressMonitor(Logger logger) {
            this.logger = requireNonNull(logger);
        }

        @Override
        public void progressStart(String task) {
        }
        //remote: Finding sources
        //Receiving objects
        //Updating references
        //Checking out files   %

        @Override
        public void progressUpdate(String task, int percentage) {
            this.logger.info(task + " " + percentage);
            if (task.contains("Receiving")) { // Receiving objects
                percentage = percentage / 2;
            } else if (task.contains("Checking")) { // Checking out files
                percentage = 50 + (percentage / 2);
            } else {
                return;
            }
            this.logger.hud(localized("fastback.hud.restore-percent", percentage));
        }

        @Override
        public void progressDone(String task) {
        }

        @Override
        public void showDuration(boolean enabled) {
        }

    }
}
