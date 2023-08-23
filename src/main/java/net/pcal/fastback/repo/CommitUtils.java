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

import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.mod.Mod;
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

import static net.pcal.fastback.config.GitConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.JGIT;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.NATIVE_GIT;
import static net.pcal.fastback.logging.UserMessage.styledLocalized;
import static net.pcal.fastback.utils.ProcessUtils.doExec;

/**
 * Utilities for adding and committing snapshots.
 *
 * @author pcal
 * @since 0.13.0
 */
class CommitUtils {

    static SnapshotId doCommitSnapshot(RepoImpl repo, Mod ctx, UserLogger log) throws IOException {
        final String uuid = repo.getWorldUuid();
        final SnapshotId newSid = SnapshotId.create(uuid);
        syslog().debug("Preparing local backup " + newSid);
        final String newBranchName = newSid.getBranchName();

        MaintenanceUtils.doPreflight(repo);

        try {

            if (repo.getConfig().getBoolean(IS_NATIVE_GIT_ENABLED)) {
                native_commit(newBranchName, repo, ctx, log);
            } else {
                jgit_commit(newBranchName, repo.getJGit(), ctx, log);
            }
        } catch (GitAPIException | InterruptedException e) {
            throw new IOException(e);
        }

        syslog().info("Local backup complete.");
        return newSid;
    }

    private static void native_commit(String newBranchName, Repo repo, Mod ctx, UserLogger log) throws IOException, InterruptedException {
        syslog().debug("Start native_commit");
        log.hud(styledLocalized("fastback.hud.local-saving", NATIVE_GIT));
        final File worktree = repo.getWorkTree();
        final Map<String,String> env = Map.of("GIT_LFS_FORCE_PROGRESS", "1");
        final Consumer<String> logConsumer = new HudConsumer(log, NATIVE_GIT);
        String[] checkout = {"git", "-C", worktree.getAbsolutePath(), "checkout", "--orphan", newBranchName};
        doExec(checkout, env, logConsumer, logConsumer);
        ctx.setWorldSaveEnabled(false);
        try {
            String[] add = {"git", "-C", worktree.getAbsolutePath(), "add", "-v", "."};
            doExec(add, env, logConsumer, logConsumer);
        } finally {
            ctx.setWorldSaveEnabled(true);
            syslog().debug("World save re-enabled.");
        }
        {
            String[] commit = {"git", "-C", worktree.getAbsolutePath(), "commit", "-m", newBranchName};
            doExec(commit, env, logConsumer, logConsumer);
        }
        syslog().debug("End native_commit");
    }

    private static void jgit_commit(String newBranchName, Git jgit, Mod ctx, final UserLogger log) throws GitAPIException {
        syslog().debug("Starting jgit_commit");
        log.hud(styledLocalized("fastback.hud.local-saving", JGIT));
        jgit.checkout().setOrphan(true).setName(newBranchName).call();
        jgit.reset().setMode(ResetCommand.ResetType.SOFT).call();
        syslog().debug("status");
        final Status status = jgit.status().call();

        try {
            syslog().debug("Disabling world save for 'git add'");
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
                    syslog().debug("Adding " + toAdd.size() + " new or modified files to index");
                    final AddCommand gitAdd = jgit.add();
                    for (final String file : toAdd) {
                        syslog().debug("add  " + file);
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
                    syslog().debug("Removing " + toDelete.size() + " deleted files from index");
                    final RmCommand gitRm = jgit.rm();
                    for (final String file : toDelete) {
                        syslog().debug("rm  " + file);
                        gitRm.addFilepattern(file);
                    }
                    gitRm.call();
                }
            }
        } finally {
            ctx.setWorldSaveEnabled(true);
            syslog().debug("World save re-enabled.");
        }
        syslog().debug("commit");
        jgit.commit().setMessage(newBranchName).call();
    }
}
