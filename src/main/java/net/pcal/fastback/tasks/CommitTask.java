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

import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.Message.localized;

@SuppressWarnings("FieldCanBeLocal")
public class CommitTask extends Task {

    private final ModContext ctx;
    private final Logger log;
    private final Git git;
    private final Supplier<SnapshotId> sidSupplier;

    public CommitTask(final Git git,
                      final ModContext ctx,
                      final Logger log) {
        this.git = requireNonNull(git);
        this.ctx = requireNonNull(ctx);
        this.log = requireNonNull(log);
        this.sidSupplier = () -> {
            try {
                return SnapshotId.create(WorldConfig.getWorldUuid(git));
            } catch (IOException e) {
                this.log.internalError("uuid lookup failed", e);
                return null;
            }
        };
    }

    public CommitTask(final Git git,
                      final ModContext ctx,
                      final Logger log,
                      final SnapshotId sid) {
        this.git = requireNonNull(git);
        this.ctx = requireNonNull(ctx);
        this.log = requireNonNull(log);
        this.sidSupplier = () -> sid;
    }

    public void run() {
        this.setStarted();
        this.log.progress(localized("fastback.notify.local-preparing"));
        final SnapshotId sid = this.sidSupplier.get();
        if (sid == null) return;
        final String newBranchName = sid.getBranchName();

// Disabling this here because https://github.com/pcal43/fastback/issues/112
//        if (ctx.isServerStopping()) {
//            log.info("Skipping save before backup because server is shutting down.");
//        } else {
//            log.info("Saving before backup");
//            ctx.saveWorld();
//            log.info("Starting backup");
//        }
        log.info("Committing " + newBranchName);
        try {
            doCommit(git, ctx, newBranchName, log);
            final Duration dur = getSplitDuration();
            log.info("Local backup complete.  Elapsed time: " + dur.toMinutesPart() + "m " + dur.toSecondsPart() + "s");
        } catch (GitAPIException | IOException e) {
            log.internalError("Local backup failed.  Unable to commit changes.", e);
            this.setFailed();
            return;
        }
        log.progress(localized("fastback.notify.backup-complete"));
        this.setCompleted();
    }

    private static void doCommit(Git git, ModContext ctx, String newBranchName, final Logger log) throws GitAPIException, IOException {
        log.debug("doing commit");
        log.debug("checkout");
        git.checkout().setOrphan(true).setName(newBranchName).call();
        git.reset().setMode(ResetCommand.ResetType.SOFT).call();
        log.debug("status");
        final Status status = git.status().call();

        try {
            log.info("Disabling world save for 'git add'");
            ctx.setWorldSaveEnabled(false);
            //
            // Figure out what files to add and remove.  We don't just 'git add .' because this:
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=494323
            //
            {
                final Collection<String> toAdd = new ArrayList<>();
                toAdd.addAll(status.getModified());
                toAdd.addAll(status.getUntracked());
                if (!toAdd.isEmpty()) {
                    log.info("Adding " + toAdd.size() + " new or modified files to index");
                    final AddCommand gitAdd = git.add();
                    for (final String file : toAdd) {
                        log.debug("add  " + file);
                        gitAdd.addFilepattern(file);
                    }
                    gitAdd.call();
                }
            }
            {
                final Collection<String> toDelete = new ArrayList<>();
                toDelete.addAll(status.getRemoved());
                toDelete.addAll(status.getMissing());
                if (!toDelete.isEmpty()) {
                    log.info("Removing " + toDelete.size() + " deleted files from index");
                    final RmCommand gitRm = git.rm();
                    for (final String file : toDelete) {
                        log.debug("rm  " + file);
                        gitRm.addFilepattern(file);
                    }
                    gitRm.call();
                }
            }
        } finally {
            ctx.setWorldSaveEnabled(true);
            log.info("World save re-enabled.");
        }
        log.debug("commit");
        log.progress(localized("fastback.notify.local-saving"));
        git.commit().setMessage(newBranchName).call();
        log.progress(localized("fastback.notify.local-done"));
    }
}
