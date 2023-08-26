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
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.utils.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.internal.storage.file.GC;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.storage.pack.PackConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.config.FastbackConfigKey.IS_BRANCH_CLEANUP_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.IS_REFLOG_DELETION_ENABLED;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.JGIT;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.NATIVE_GIT;
import static net.pcal.fastback.logging.UserMessage.raw;
import static net.pcal.fastback.logging.UserMessage.styledLocalized;
import static net.pcal.fastback.logging.UserMessage.styledRaw;
import static net.pcal.fastback.repo.PushUtils.isTempBranch;
import static net.pcal.fastback.utils.ProcessUtils.doExec;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.apache.commons.io.FileUtils.sizeOfDirectory;
import static org.eclipse.jgit.api.ListBranchCommand.ListMode.ALL;

/**
 * Utilities for reclaiming disk space from pruned branches.
 *
 * @author pcal
 * @since 0.13.0
 */
class ReclamationUtils {

    static void doReclamation(RepoImpl repo, UserLogger ulog) throws IOException {
        if (repo.getConfig().getBoolean(IS_NATIVE_GIT_ENABLED)) {
            try {
                native_doLfsPrune(repo, ulog);
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        } else {
            try {
                jgit_doGc(repo, ulog);
            } catch (GitAPIException | ParseException e) {
                throw new IOException(e);
            }
        }
    }

    private static void native_doLfsPrune(RepoImpl repo, UserLogger ulog) throws IOException, InterruptedException {
        final File worktree = repo.getWorkTree();
        final String[] push = {"git", "-C", worktree.getAbsolutePath(), "-c", "lfs.pruneoffsetdays=999999", "lfs", "prune", "--verbose", "--no-verify-remote",};
        final Consumer<String> outputConsumer = line->ulog.update(styledRaw(line, NATIVE_GIT));
        doExec(push, Collections.emptyMap(), outputConsumer, outputConsumer);
        syslog().debug("native_doLfsPrune");
    }

    /**
     * Runs git garbage collection.  Aggressively deletes reflogs, tracking branches and stray temporary branches
     * in an attempt to free up objects and reclaim disk space.
     */
    private static void jgit_doGc(RepoImpl repo, UserLogger ulog) throws IOException, GitAPIException, ParseException {
        final File gitDir = repo.getJGit().getRepository().getDirectory();
        final GitConfig config = repo.getConfig();
        ulog.update(styledLocalized("fastback.hud.gc-percent", JGIT, 0));
        syslog().debug("Stats before gc:");
        syslog().debug(String.valueOf(repo.getJGit().gc().getStatistics()));
        final long sizeBeforeBytes = sizeOfDirectory(gitDir);
        syslog().info("Backup size before gc: " + byteCountToDisplaySize(sizeBeforeBytes));
        if (config.getBoolean(IS_REFLOG_DELETION_ENABLED)) {
            // reflogs aren't very useful in our case and cause old snapshots to get retained
            // longer than people expect.
            final Path reflogsDir = gitDir.toPath().resolve("logs");
            syslog().debug("Deleting reflogs " + reflogsDir);
            FileUtils.rmdir(reflogsDir);
        }
        if (config.getBoolean(IS_BRANCH_CLEANUP_ENABLED)) {
            final List<String> branchesToDelete = new ArrayList<>();
            for (final Ref ref : repo.getJGit().branchList().setListMode(ALL).call()) {
                final String branchName = BranchUtils.getBranchName(ref);
                if (branchName == null) {
                    syslog().warn("Non-branch ref returned by branchList: " + ref);
                } else if (isTempBranch(branchName)) {
                    branchesToDelete.add(branchName);
                } else if (repo.getSidCodec().isSnapshotBranchName(repo.getWorldId(), branchName)) {
                    // ok fine
                } else {
                    syslog().warn("Unidentified branch found " + branchName + " - consider removing it with 'git branch -D'");
                }
            }
            if (branchesToDelete.isEmpty()) {
                syslog().debug("No branches to clean up");
            } else {
                syslog().debug("Deleting branches: " + branchesToDelete);
                repo.deleteLocalBranches(branchesToDelete);
                syslog().debug("Branches deleted.");
            }
        }
        final GC gc = new GC(((FileRepository) repo.getJGit().getRepository()));
        gc.setExpireAgeMillis(0);
        gc.setPackExpireAgeMillis(0);
        gc.setAuto(false);
        final PackConfig pc = new PackConfig();
        pc.setDeltaCompress(false);
        gc.setPackConfig(pc);
        final ProgressMonitor pm = new JGitIncrementalProgressMonitor(new GcProgressMonitor(ulog), 100);
        gc.setProgressMonitor(pm);
        syslog().debug("Starting garbage collection");
        gc.gc(); // TODO progress monitor
        syslog().debug("Garbage collection complete.");
        syslog().debug("Stats after gc:");
        syslog().debug("" + repo.getJGit().gc().getStatistics());
        final long sizeAfterBytes = sizeOfDirectory(gitDir);
        syslog().info("Backup size after gc: " + byteCountToDisplaySize(sizeAfterBytes));
    }

    private static class GcProgressMonitor extends JGitPercentageProgressMonitor {

        private final UserLogger ulog;

        public GcProgressMonitor(UserLogger ulog) {
            this.ulog = requireNonNull(ulog);
        }

        @Override
        public void progressStart(String task) {
            this.ulog.update(raw(task));
        }

        @Override
        public void progressUpdate(String task, int percentage) {
            final String message = task + " " + percentage + "%";
            syslog().debug(message);
            this.ulog.update(styledLocalized(message, JGIT));
        }

        @Override
        public void progressDone(String task) {
            final String message = "Done " + task;
            syslog().debug(message);
            this.ulog.update(styledLocalized(message, JGIT)); // FIXME i18n?
        }

        @Override
        public void showDuration(boolean enabled) {
        }
    }
}
