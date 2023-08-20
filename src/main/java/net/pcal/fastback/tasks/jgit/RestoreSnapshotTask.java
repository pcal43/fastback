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

package net.pcal.fastback.tasks.jgit;

import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.progress.IncrementalProgressMonitor;
import net.pcal.fastback.progress.PercentageProgressMonitor;
import net.pcal.fastback.utils.FileUtils;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ProgressMonitor;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.config.RepoConfigUtils.WORLD_UUID_PATH;
import static net.pcal.fastback.logging.Message.localized;

@SuppressWarnings("FieldCanBeLocal")
public class RestoreSnapshotTask implements Callable<Path> {

    private final String repoUri;
    private final SnapshotId sid;
    private final Path restoreTargetDir;
    private final String worldName;
    private final Logger logger;

    public RestoreSnapshotTask(String repoUri, Path saveDir, String worldName, SnapshotId sid, Logger logger) {
        this.repoUri = requireNonNull(repoUri);
        this.restoreTargetDir = requireNonNull(saveDir);
        this.sid = requireNonNull(sid);
        this.worldName = requireNonNull(worldName);
        this.logger = requireNonNull(logger);
    }


    @Override
    public Path call() throws Exception {
        final Path restoreDir = getTargetDir(this.restoreTargetDir, this.worldName, this.sid.getName());
        final String branchName = sid.getBranchName();
            this.logger.hud(localized("fastback.hud.restore-percent", 0));
            final ProgressMonitor pm = new IncrementalProgressMonitor(new RestoreProgressMonitor(logger), 100);
            try (Git git = Git.cloneRepository().setProgressMonitor(pm).setDirectory(restoreDir.toFile()).
                    setBranchesToClone(List.of("refs/heads/" + branchName)).setBranch(branchName).setURI(this.repoUri).call()) {
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

    private static class RestoreProgressMonitor extends PercentageProgressMonitor {

        private final Logger logger;

        public RestoreProgressMonitor(Logger logger) {
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
