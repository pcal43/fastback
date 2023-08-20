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

import net.pcal.fastback.ModContext;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.FileUtils;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.internal.storage.file.GC;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.storage.pack.PackConfig;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.repo.DoPush.isTempBranch;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.apache.commons.io.FileUtils.sizeOfDirectory;
import static org.eclipse.jgit.api.ListBranchCommand.ListMode.ALL;

/**
 * Runs git garbage collection.  Aggressively deletes reflogs, tracking branches and stray temporary branches
 * in an attempt to free up objects and reclaim disk space.
 *
 * @author pcal
 * @since 0.0.12
 */
class JGitGcTask implements Callable<Void> {

    private final ModContext ctx;
    private final Logger log;
    private final RepoImpl repo;
    private long sizeBeforeBytes, sizeAfterBytes;

    JGitGcTask(final RepoImpl repo,
               final ModContext ctx,
               final Logger log) {
        this.repo = requireNonNull(repo);
        this.ctx = requireNonNull(ctx);
        this.log = requireNonNull(log);
    }

    @Deprecated
    public static String getBranchName(Ref fromBranchRef) {
        final String REFS_HEADS = "refs/heads/";
        final String name = fromBranchRef.getName();
        if (name.startsWith(REFS_HEADS)) {
            return name.substring(REFS_HEADS.length());
        } else {
            return null;
        }
    }

    @Override
    public Void call() throws Exception {
        final File gitDir = repo.getJGit().getRepository().getDirectory();
        log.hud(localized("fastback.hud.gc-percent", 0));
        log.info("Stats before gc:");
        log.info("" + repo.getJGit().gc().getStatistics());
        this.sizeBeforeBytes = sizeOfDirectory(gitDir);
        log.info("Backup size before gc: " + byteCountToDisplaySize(sizeBeforeBytes));
        if (ctx.isReflogDeletionEnabled()) {
            // reflogs aren't very useful in our case and cause old snapshots to get retained
            // longer than people expect.
            final Path reflogsDir = gitDir.toPath().resolve("logs");
            log.info("Deleting reflogs " + reflogsDir);
            FileUtils.rmdir(reflogsDir);
        }
        if (ctx.isBranchCleanupEnabled()) {
            final List<String> branchesToDelete = new ArrayList<>();
            for (final Ref ref : repo.getJGit().branchList().setListMode(ALL).call()) {
                final String branchName = getBranchName(ref);
                if (branchName == null) {
                    log.warn("Non-branch ref returned by branchList: " + ref);
                } else if (isTempBranch(branchName)) {
                    branchesToDelete.add(branchName);
                } else if (SnapshotId.isSnapshotBranchName(branchName)) {
                    // ok fine
                } else {
                    log.warn("Unidentified branch found " + branchName + " - consider removing it with 'git branch -D'");
                }
            }
            if (branchesToDelete.isEmpty()) {
                log.info("No branches to clean up");
            } else {
                log.info("Deleting branches: " + branchesToDelete);
                repo.getJGit().branchDelete().setForce(true).setBranchNames(branchesToDelete.toArray(new String[0])).call();
                log.info("Branches deleted.");
            }
        }
        final GC gc = new GC(((FileRepository) repo.getJGit().getRepository()));
        gc.setExpireAgeMillis(0);
        gc.setPackExpireAgeMillis(0);
        gc.setAuto(false);
        final PackConfig pc = new PackConfig();
        pc.setDeltaCompress(false);
        gc.setPackConfig(pc);
        final ProgressMonitor pm = new JGitIncrementalProgressMonitor(new GcProgressMonitor(this.log), 100);
        gc.setProgressMonitor(pm);
        log.info("Starting garbage collection");
        gc.gc(); // TODO progress monitor
        log.info("Garbage collection complete.");
        log.info("Stats after gc:");
        log.info("" + repo.getJGit().gc().getStatistics());
        this.sizeAfterBytes = sizeOfDirectory(gitDir);
        log.info("Backup size after gc: " + byteCountToDisplaySize(sizeAfterBytes));
        return null;
    }

    public long getBytesReclaimed() {
        return this.sizeBeforeBytes - this.sizeAfterBytes;
    }

    private static class GcProgressMonitor extends JGitPercentageProgressMonitor {

        private final Logger logger;

        public GcProgressMonitor(Logger logger) {
            this.logger = requireNonNull(logger);
        }

        @Override
        public void progressStart(String task) {
            this.logger.info(task);
        }

        @Override
        public void progressUpdate(String task, int percentage) {
            this.logger.info(task + " " + percentage + "%");
            // Pack refs
            // Finding sources
            // Writing objects
            // Selecting commits
            // Building bitmaps
            // Prune loose objects
            // Prune loose objects also found in pack files
            // Prune loose, unreferenced objects
            if (task.contains("Writing objects")) {
                this.logger.hud(localized("fastback.hud.gc-percent", (int) (percentage * 9 / 10)));
            } else if (task.contains("Selecting commits")) {
                this.logger.hud(localized("fastback.hud.gc-percent", 90 + (int) (percentage / 20)));
            } else if (task.contains("Prune loose objects")) {
                this.logger.hud(localized("fastback.hud.gc-percent", 95 + (int) (percentage / 20)));
            }
        }

        @Override
        public void progressDone(String task) {
            logger.info("Done " + task);
        }

        @Override
        public void showDuration(boolean enabled) {
        }
    }
}
