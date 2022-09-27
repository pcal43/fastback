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
import net.pcal.fastback.logging.IncrementalProgressMonitor;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.LoggingProgressMonitor;
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
                logger.notifyError(localized("fastback.notify.restore-nosuch", snapshotName));
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
            logger.notify(localized("fastback.notify.restore-start", this.snapshotName, targetDirectory));
            final ProgressMonitor pm = new IncrementalProgressMonitor(new LoggingProgressMonitor(logger), 100);
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
        logger.notify(localized("fastback.notify.restore-done"));
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
}
