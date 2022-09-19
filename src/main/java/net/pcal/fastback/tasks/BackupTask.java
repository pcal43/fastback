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
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Objects.requireNonNull;
import static net.minecraft.text.Text.translatable;

@SuppressWarnings("FieldCanBeLocal")
public class BackupTask extends Task {

    private final Path worldSaveDir;
    private final Logger log;

    public BackupTask(final Path worldSaveDir, final Logger log) {
        this.worldSaveDir = requireNonNull(worldSaveDir);
        this.log = requireNonNull(log);
    }

    public void run() {
        this.setStarted();
        this.log.notify(translatable("fastback.notify.local-preparing"));
        try (Git git = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
            final WorldConfig config;
            try {
                config = WorldConfig.load(worldSaveDir, git.getRepository().getConfig());
            } catch (IOException e) {
                log.internalError("Local backup failed.  Could not determine world-uuid.", e);
                this.setFailed();
                return;
            }
            final SnapshotId newSid = SnapshotId.create(config.worldUuid());
            final String newBranchName = newSid.getBranchName();
            log.info("Committing " + newBranchName);
            try {
                doCommit(git, config, newBranchName, log);
                final Duration dur = getSplitDuration();
                log.info("Local backup complete.  Elapsed time: " + dur.toMinutesPart() + "m " + dur.toSecondsPart() + "s");
                this.log.notify(translatable("fastback.notify.local-done"));
            } catch (GitAPIException | IOException e) {
                log.internalError("Local backup failed.  Unable to commit changes.", e);
                this.setFailed();
                return;
            }
            if (config.isRemoteBackupEnabled()) {
                this.log.notify(translatable("fastback.notify.push-started"));
                final PushTask push = new PushTask(worldSaveDir, newBranchName, log);
                push.run();
                if (push.isFailed()) {
                    log.notifyError("Local backup succeeded but remote backup failed.  See log for details.");
                } else {
                    final Duration dur = getSplitDuration();
                    log.info("Remote backup to complete");
                    log.info("Elapsed time: " + dur.toMinutesPart() + "m " + dur.toSecondsPart() + "s");
                }
            } else {
                log.info("Remote backup disabled.");
            }
        } catch (GitAPIException e) {
            log.internalError("Backup failed unexpectedly", e);
            this.setFailed();
            return;
        }
        this.setCompleted();
    }

    private static void doCommit(Git git, WorldConfig config, String newBranchName, final Logger log) throws GitAPIException, IOException {
        log.debug("doing commit");
        log.debug("checkout");
        git.checkout().setOrphan(true).setName(newBranchName).call();
        git.reset().setMode(ResetCommand.ResetType.SOFT).call();
        log.debug("status");
        final Status status = git.status().call();

        log.debug("add");

        //
        // Figure out what files to add and remove.  We don't just 'git add .' because this:
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=494323
        //
        {
            final Collection<String> toAdd = new ArrayList<>();
            toAdd.addAll(status.getModified());
            toAdd.addAll(status.getUntracked());
            if (!toAdd.isEmpty()) {
                final AddCommand gitAdd = git.add();
                log.debug("doing add");
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
                log.debug("doing rm");
                final RmCommand gitRm = git.rm();
                for (final String file : toDelete) {
                    log.debug("rm  " + file);
                    gitRm.addFilepattern(file);
                }
                gitRm.call();
            }
        }
        log.debug("commit");
        log.notify(translatable("fastback.notify.local-saving"));
        git.commit().setMessage(newBranchName).call();
        log.notify(translatable("fastback.notify.local-done"));
    }
}
