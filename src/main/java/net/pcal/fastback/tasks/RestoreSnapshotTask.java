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

package net.pcal.fastback.tasks;

import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.Message;
import net.pcal.fastback.progress.IncrementalProgressMonitor;
import net.pcal.fastback.progress.PercentageProgressMonitor;
import net.pcal.fastback.utils.FileUtils;
import net.pcal.fastback.utils.GitUtils;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.WorldConfig.WORLD_UUID_PATH;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.logging.Message.raw;

@SuppressWarnings("FieldCanBeLocal")
public class RestoreSnapshotTask extends Task {

    private final Path worldSaveDir;
    private final String snapshotName;
    private final String worldName;
    private final Path saveDir;
    private final boolean doClean = true;
    private final Logger logger;
    private Path restoreDir;

    public static RestoreSnapshotTask create(Path worldSaveDir,
                                             String snapshotName,
                                             String worldName,
                                             Path saveDir,
                                             Logger logger) {
        return new RestoreSnapshotTask(worldSaveDir, snapshotName, worldName, saveDir, logger);
    }

    private RestoreSnapshotTask(Path worldSaveDir, String snapshotName, String worldName, Path saveDir, Logger logger) {
        this.worldSaveDir = requireNonNull(worldSaveDir);
        this.snapshotName = requireNonNull(snapshotName);
        this.worldName = requireNonNull(worldName);
        this.saveDir = requireNonNull(saveDir);
        this.logger = requireNonNull(logger);
    }

    public void run() {
        setStarted();
        final WorldConfig config;
        final String branchName;
        try (final Git git = Git.open(this.worldSaveDir.toFile())) {
            config = WorldConfig.load(git);
            SnapshotId sid = SnapshotId.fromUuidAndName(config.worldUuid(), this.snapshotName);
            branchName = sid.getBranchName();
            if (!GitUtils.isBranchExtant(git, branchName, logger)) {
                logger.chatError(localized("fastback.chat.restore-nosuch", snapshotName));
                return;
            }
        } catch (IOException | GitAPIException | ParseException e) {
            logger.internalError("Unexpected error looking up branch names", e);
            setFailed();
            return;
        }

        try {
            restoreDir = getTargetDir(this.saveDir, worldName, snapshotName);
            String uri = "file://" + this.worldSaveDir.toAbsolutePath();
            this.logger.hud(localized("fastback.hud.restore-percent", 0));
            final ProgressMonitor pm = new IncrementalProgressMonitor(new RestoreProgressMonitor(logger), 100);
            try (Git git = Git.cloneRepository().setProgressMonitor(pm).setDirectory(restoreDir.toFile()).
                    setBranchesToClone(List.of("refs/heads/" + branchName)).setBranch(branchName).setURI(uri).call()) {
            }
        } catch (Exception e) {
            logger.internalError("Restoration clone of " + branchName + " failed.", e);
            setFailed();
            return;
        }
        if (config.isPostRestoreCleanupEnabled()) {
            try {
                FileUtils.rmdir(restoreDir.resolve(".git"));
                restoreDir.resolve(WORLD_UUID_PATH).toFile().delete();
            } catch (IOException e) {
                logger.internalError("Unexpected error cleaning restored snapshot", e);
                setFailed();
                return;
            }
        }
        setCompleted();
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

    public Path getRestoreDir() {
        return this.restoreDir;
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
            this.logger.info(task +  " "+percentage);
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
    }
}
