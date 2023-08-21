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
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import static net.pcal.fastback.config.GitConfigKey.IS_NATIVE_ENABLED;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.utils.ExecUtils.doExec;

@SuppressWarnings("FieldCanBeLocal")
class DoCommit {

    static SnapshotId doCommitSnapshot(RepoImpl repo, ModContext ctx, Logger log) throws IOException {
        log.hud(localized("fastback.hud.local-saving"));
        final String uuid = repo.getWorldUuid();
        final SnapshotId newSid = SnapshotId.create(uuid);
        log.info("Preparing local backup " + newSid);
        final String newBranchName = newSid.getBranchName();

        try {
            if (repo.getConfig().getBoolean(IS_NATIVE_ENABLED)) {
                native_commit(newBranchName, repo, ctx, log);
            } else {
                jgit_commit(newBranchName, repo.getJGit(), ctx, log);
            }
        } catch (GitAPIException | InterruptedException e) {
            throw new IOException(e);
        }

        log.info("Local backup complete.");
        return newSid;
    }

    private static void native_commit(String newBranchName, Repo repo, ModContext ctx, Logger log) throws IOException, InterruptedException {
        log.debug("Start native_commit");
        final File worktree = repo.getWorkTree();
        final Map<String,String> env = Map.of("GIT_LFS_FORCE_PROGRESS", "1");
        final Consumer<String> logConsumer = new LogConsumer(log);
        String[] checkout = {"git", "-C", worktree.getAbsolutePath(), "checkout", "--orphan", newBranchName};
        doExec(checkout, env, logConsumer, logConsumer, log);
        ctx.setWorldSaveEnabled(false);
        try {
            String[] add = {"git", "-C", worktree.getAbsolutePath(), "add", "-v", "."};
            doExec(add, env, logConsumer, logConsumer, log);
        } finally {
            ctx.setWorldSaveEnabled(true);
            log.debug("World save re-enabled.");
        }
        {
            String[] commit = {"git", "-C", worktree.getAbsolutePath(), "commit", "-m", newBranchName};
            doExec(commit, env, logConsumer, logConsumer, log);
        }
        log.debug("End native_commit");
    }

    private static void jgit_commit(String newBranchName, Git jgit, ModContext ctx, final Logger log) throws GitAPIException {
        log.debug("Starting jgit_commit");
        jgit.checkout().setOrphan(true).setName(newBranchName).call();
        jgit.reset().setMode(ResetCommand.ResetType.SOFT).call();
        log.debug("status");
        final Status status = jgit.status().call();

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
                    final AddCommand gitAdd = jgit.add();
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
                    final RmCommand gitRm = jgit.rm();
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
        jgit.commit().setMessage(newBranchName).call();
    }
}
