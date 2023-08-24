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
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static net.pcal.fastback.config.GitConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.JGIT;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.NATIVE_GIT;
import static net.pcal.fastback.logging.UserMessage.styledLocalized;
import static net.pcal.fastback.logging.UserMessage.styledRaw;
import static net.pcal.fastback.mod.Mod.mod;
import static net.pcal.fastback.utils.ProcessUtils.doExec;

/**
 * Utilities for adding and committing snapshots.
 *
 * @author pcal
 * @since 0.13.0
 */
class CommitUtils {

    public static SnapshotId doCommitSnapshot(final RepoImpl repo, final UserLogger ulog) throws IOException {
        MaintenanceUtils.doPreflight(repo);
        final String uuid = repo.getWorldUuid();
        final SnapshotId newSid = SnapshotId.create(uuid);
        syslog().debug("start doCommitSnapshot for "+newSid);
        final String newBranchName = newSid.getBranchName();
        try {
            if (repo.getConfig().getBoolean(IS_NATIVE_GIT_ENABLED)) {
                native_commit(newBranchName, repo, ulog);
            } else {
                jgit_commit(newBranchName, repo.getJGit(), ulog);
            }
        } catch (GitAPIException | InterruptedException e) {
            throw new IOException(e);
        }
        syslog().debug("Local backup complete.");
        return newSid;
    }

    private static void native_commit(final String newBranchName, final Repo repo, final UserLogger log) throws IOException, InterruptedException {
        syslog().debug("Start native_commit");
        log.update(styledLocalized("fastback.hud.local-saving", NATIVE_GIT));
        final File worktree = repo.getWorkTree();
        final Map<String, String> env = Map.of("GIT_LFS_FORCE_PROGRESS", "1");
        final Consumer<String> outputConsumer = line->log.update(styledRaw(line, NATIVE_GIT));
        String[] checkout = {"git", "-C", worktree.getAbsolutePath(), "checkout", "--orphan", newBranchName};
        doExec(checkout, env, outputConsumer, outputConsumer);
        mod().setWorldSaveEnabled(false);
        try {
            String[] add = {"git", "-C", worktree.getAbsolutePath(), "add", "-v", "."};
            doExec(add, env, outputConsumer, outputConsumer);
        } finally {
            mod().setWorldSaveEnabled(true);
            syslog().debug("World save re-enabled.");
        }
        {
            String[] commit = {"git", "-C", worktree.getAbsolutePath(), "commit", "-m", newBranchName};
            doExec(commit, env, outputConsumer, outputConsumer);
        }
        syslog().debug("End native_commit");
    }

    private static void jgit_commit(final String newBranchName, final Git jgit, final UserLogger ulog) throws GitAPIException {
        syslog().debug("Starting jgit_commit");
        ulog.update(styledLocalized("fastback.hud.local-saving", JGIT));
        jgit.checkout().setOrphan(true).setName(newBranchName).call();
        jgit.reset().setMode(ResetCommand.ResetType.SOFT).call();
        syslog().debug("status");
        final Status status = jgit.status().call();

        try {
            syslog().debug("Disabling world save for 'git add'");
            mod().setWorldSaveEnabled(false);
            //
            // Figure out what files to add and remove.  We don't just 'git add .' because this:
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=494323
            //
            {
                final List<String> toAdd = new ArrayList<>();
                toAdd.addAll(status.getModified());
                toAdd.addAll(status.getUntracked());
                Collections.sort(toAdd);
                if (!toAdd.isEmpty()) {
                    syslog().debug("Adding " + toAdd.size() + " new or modified files to index");

                    for (final String file : toAdd) {
                        final AddCommand gitAdd = jgit.add();
                        syslog().debug("add  " + file);
                        ulog.update(styledRaw("Backing up " + file, JGIT)); //FIXME i18n
                        gitAdd.addFilepattern(file);
                        gitAdd.call();
                    }
                }
            }
            {
                final List<String> toDelete = new ArrayList<>();
                toDelete.addAll(status.getRemoved());
                toDelete.addAll(status.getMissing());
                Collections.sort(toDelete);
                if (!toDelete.isEmpty()) {
                    syslog().debug("Removing " + toDelete.size() + " deleted files from index");
                    for (final String file : toDelete) {
                        final RmCommand gitRm = jgit.rm();
                        syslog().debug("rm  " + file);
                        ulog.update(styledRaw("Removing " + file, JGIT)); //FIXME i18n
                        gitRm.addFilepattern(file);
                        gitRm.call();
                    }
                }
            }
        } finally {
            mod().setWorldSaveEnabled(true);
            syslog().debug("World save re-enabled.");
        }
        syslog().debug("commit");
        ulog.update(styledRaw("Commit complete", JGIT)); //FIXME i18n
        jgit.commit().setMessage(newBranchName).call();
    }
}
