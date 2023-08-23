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

import com.google.common.collect.ListMultimap;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mod.Mod;
import net.pcal.fastback.utils.EnvironmentUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Ref;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.config.GitConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.REMOTE_NAME;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.raw;

/**
 * @author pcal
 * @since 0.13.0
 */
class RepoImpl implements Repo {

    // ======================================================================
    // Constants

    static final Path WORLD_UUID_PATH = Path.of("fastback/world.uuid");

    // ======================================================================
    // Fields

    private final Git jgit;
    private final Mod ctx;
    private GitConfig config;

    // ======================================================================
    // Constructors

    RepoImpl(final Git git,
             final Mod ctx) {
        this.jgit = requireNonNull(git);
        this.ctx = requireNonNull(ctx);
    }

    // ======================================================================
    // Repo implementation

    @Override
    public void doCommitAndPush(final UserLogger ulog) throws IOException {
        if (!doNativeCheck(ulog)) return;
        saveWorldBeforeBackup(ulog);
        final SnapshotId newSid = CommitUtils.doCommitSnapshot(this, ctx, ulog);
        PushUtils.doPush(newSid, this, ulog);
        ulog.chat(UserMessage.localized("fastback.chat.backup-complete"));//FIXME not if it failed
    }

    @Override
    public void doCommitSnapshot(final UserLogger ulog) throws IOException {
        if (!doNativeCheck(ulog)) return;
        saveWorldBeforeBackup(ulog);
        CommitUtils.doCommitSnapshot(this, ctx, ulog);
        ulog.chat(UserMessage.localized("fastback.chat.backup-complete")); //FIXME not necessarily
    }

    @Override
    public Collection<SnapshotId> doLocalPrune(final UserLogger ulog) throws IOException {
        return PruneUtils.doLocalPrune(this, ulog);
    }

    @Override
    public Collection<SnapshotId> doRemotePrune(final UserLogger ulog) throws IOException {
        return PruneUtils.doRemotePrune(this, ulog);
    }

    @Override
    public void doGc(final UserLogger ulog) throws IOException {
        if (!doNativeCheck(ulog)) return;
        ReclamationUtils.doReclamation(this, ulog);
    }

    @Override
    public Path doRestoreSnapshot(String uri, Path restoresDir, String worldName, SnapshotId sid, UserLogger ulog) throws IOException {
        return RestoreUtils.restoreSnapshot(uri, restoresDir, worldName, sid, ulog);
    }

    @Override
    public String getWorldUuid() throws IOException {
        return Files.readString(getWorkTree().toPath().toAbsolutePath().resolve(WORLD_UUID_PATH)).trim();
    }

    @Override
    public ListMultimap<String, SnapshotId> listSnapshots() throws IOException {
        final JGitSupplier<Collection<Ref>> refProvider = () -> {
            try {
                return jgit.branchList().call();
            } catch (GitAPIException e) {
                throw new IOException(e);
            }
        };
        try {
            return new ListSnapshotsTask(refProvider).call();
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }

    @Override
    public ListMultimap<String, SnapshotId> listRemoteSnapshots() throws IOException {
        final GitConfig conf = GitConfig.load(jgit);
        final String remoteName = conf.getString(REMOTE_NAME);
        final JGitSupplier<Collection<Ref>> refProvider = () -> {
            try {
                return jgit.lsRemote().setRemote(remoteName).setHeads(true).call();
            } catch (GitAPIException e) {
                throw new IOException(e);
            }
        };
        try {
            return new ListSnapshotsTask(refProvider).call();
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }

    @Override
    public GitConfig getConfig() {
        if (this.config == null) {
            this.config = GitConfig.load(this.jgit);
        }
        return this.config;
    }

    @Override
    public File getDirectory() throws NoWorkTreeException {
        return this.jgit.getRepository().getDirectory();
    }

    @Override
    public File getWorkTree() throws NoWorkTreeException {
        return this.jgit.getRepository().getWorkTree();
    }

    @Override
    public void deleteRemoteBranch(String remoteBranchName) throws IOException {
        PruneUtils.deleteRemoteBranch(this, remoteBranchName);
    }

    @Override
    public void deleteLocalBranches(List<String> branchesToDelete) throws IOException {
        PruneUtils.deleteLocalBranches(this, branchesToDelete);
    }

    Git getJGit() {
        return this.jgit;
    }

    @Override
    public void close() {
        this.getJGit().close();
    }

    @Override
    public void setNativeGitEnabled(boolean enabled, UserLogger userlog) throws IOException {
        MaintenanceUtils.setNativeGitEnabled(enabled, this, userlog);
    }

    // ======================================================================
    // Private

    private void saveWorldBeforeBackup(UserLogger ulog) {
        // workaround for https://github.com/pcal43/fastback/issues/112
        ulog.hud(raw("Saving world before backup...")); //FIXME i18n
        syslog().info("Saving before backup");
        ctx.saveWorld();
        syslog().info("Starting backup..."); //FIXME i18n
    }

    private boolean doNativeCheck(UserLogger ulog) {
        final GitConfig config = this.getConfig();
        if (config.getBoolean(IS_NATIVE_GIT_ENABLED)) {
            if (!EnvironmentUtils.isNativeGitInstalled()) {
                ulog.chat(UserMessage.rawError("Unable to backup: native mode enabled but git is not installed."));
                return false;
            }
        }
        return true;
    }

}
