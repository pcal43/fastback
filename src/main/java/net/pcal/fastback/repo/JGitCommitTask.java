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
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.Message.localized;

@SuppressWarnings("FieldCanBeLocal")
class JGitCommitTask implements Callable<SnapshotId> {

    private final ModContext ctx;
    private final Logger log;
    private final RepoImpl repo;

    JGitCommitTask(final RepoImpl repo,
                   final ModContext ctx,
                   final Logger log) {
        this.repo = requireNonNull(repo);
        this.ctx = requireNonNull(ctx);
        this.log = requireNonNull(log);
    }

    @Override
    public SnapshotId call() throws GitAPIException, IOException {
        this.log.hud(localized("fastback.hud.local-saving"));
        final String uuid = repo.getWorldUuid();
        final SnapshotId newSid = SnapshotId.create(uuid);
        log.info("Preparing local backup " + newSid);
        final String newBranchName = newSid.getBranchName();
        doCommit(repo.getJGit(), ctx, newBranchName, log);
        log.info("Local backup complete.");
        return newSid;
    }

    private static void doCommit(Git git, ModContext ctx, String newBranchName, final Logger log) throws GitAPIException {
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
        git.commit().setMessage(newBranchName).call();
    }
}
