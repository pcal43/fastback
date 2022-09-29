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
import net.pcal.fastback.logging.Message;
import net.pcal.fastback.progress.IncrementalProgressMonitor;
import net.pcal.fastback.logging.Logger;
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

    public static Runnable create(Path worldSaveDir,
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
                logger.chatError(localized("fastback.notify.restore-nosuch", snapshotName));
                return;
            }
        } catch (IOException | GitAPIException | ParseException e) {
            logger.internalError("Unexpected error looking up branch names", e);
            setFailed();
            return;
        }

        final Path targetDirectory;
        try {
            targetDirectory = getTargetDir(this.saveDir, worldName, snapshotName);
            String uri = "file://" + this.worldSaveDir.toAbsolutePath();
            logger.progress(localized("fastback.notify.restore-start", this.snapshotName, targetDirectory));
            final ProgressMonitor pm = new IncrementalProgressMonitor(new RestoreProgressMonitor(logger), 100);
            try (Git git = Git.cloneRepository().setProgressMonitor(pm).setDirectory(targetDirectory.toFile()).
                    setBranchesToClone(List.of("refs/heads/" + branchName)).setBranch(branchName).setURI(uri).call()) {
            }
        } catch (Exception e) {
            logger.internalError("Restoration clone of " + branchName + " failed.", e);
            setFailed();
            return;
        }
        if (config.isPostRestoreCleanupEnabled()) {
            try {
                FileUtils.rmdir(targetDirectory.resolve(".git"));
                targetDirectory.resolve(WORLD_UUID_PATH).toFile().delete();
            } catch (IOException e) {
                logger.internalError("Unexpected error cleaning restored snapshot", e);
                setFailed();
                return;
            }
        }
        logger.progress(localized("fastback.notify.restore-done"));
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


    private static class RestoreProgressMonitor extends PercentageProgressMonitor {

        private final Logger logger;

        public RestoreProgressMonitor(Logger logger) {
            this.logger = requireNonNull(logger);
        }

        @Override
        public void progressStart(String task) {
            this.logger.info(task);
        }

        @Override
        public void progressUpdate(String task, int percentage) {
            Message text = null;
            // FIXME these are wrong
            if (task.contains("Finding sources")) {
                text = localized("fastback.savescreen.remote-preparing", percentage);
            } else if (task.contains("Writing objects")) {
                text = localized("fastback.savescreen.remote-uploading", percentage);
            }
            if (text == null) text = raw(task + " " + percentage + "%");
            this.logger.progress(text);
        }

        @Override
        public void progressDone(String task) {
            final Message text;
            if (task.contains("Writing objects")) {
                text = localized("fastback.savescreen.remote-done");
            } else {
                text = raw(task);
            }
            this.logger.progress(text);
        }
    }
}
