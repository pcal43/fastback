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
import net.pcal.fastback.utils.EnvironmentUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Ref;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.config.GitConfigKey.BROADCAST_NOTICE_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.BROADCAST_NOTICE_MESSAGE;
import static net.pcal.fastback.config.GitConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.REMOTE_NAME;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.BROADCAST;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.logging.UserMessage.localized;
import static net.pcal.fastback.logging.UserMessage.styledLocalized;
import static net.pcal.fastback.logging.UserMessage.styledRaw;
import static net.pcal.fastback.mod.Mod.mod;

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
    private GitConfig config;

    // ======================================================================
    // Constructors

    RepoImpl(final Git jgit) {
        this.jgit = requireNonNull(jgit);
    }

    // ======================================================================
    // Repo implementation

    @Override
    public void doCommitAndPush(final UserLogger ulog) {
        if (!doNativeCheck(ulog)) return;
        broadcastBackupNotice();
        final long start = System.currentTimeMillis();
        final SnapshotId newSid;
        try {
            newSid = CommitUtils.doCommitSnapshot(this, ulog);
        } catch(IOException ioe) {
            ulog.message(styledLocalized("fastback.chat.commit-failed", ERROR));
            return;
        }
        try {
            PushUtils.doPush(newSid, this, ulog);
        } catch(IOException ioe) {
            ulog.message(styledLocalized("fastback.chat.push-failed", ERROR));
            syslog().error(ioe);
            return;
        }
        ulog.message(localized("fastback.chat.backup-complete-elapsed", getDuration(start)));
    }

    @Override
    public void doCommitSnapshot(final UserLogger ulog) {
        if (!doNativeCheck(ulog)) return;
        broadcastBackupNotice();
        final long start = System.currentTimeMillis();
        try {
            CommitUtils.doCommitSnapshot(this, ulog);
        } catch(IOException ioe) {
            ulog.message(styledLocalized("fastback.chat.commit-failed", ERROR));
            syslog().error(ioe);
            return;
        }
        ulog.message(localized("fastback.chat.backup-complete-elapsed", getDuration(start)));
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
        final Path uuidPath = getWorkTree().toPath().resolve(WORLD_UUID_PATH);
        if (!uuidPath.toFile().exists()) throw new FileNotFoundException(uuidPath.toString());
        return Files.readString(uuidPath).trim();
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

    private static String getDuration(long since) {
        final Duration d = Duration.of(System.currentTimeMillis() - since, ChronoUnit.MILLIS);
        long seconds = d.getSeconds();
        if (seconds < 60) {
            return String.format("%ds", seconds == 0 ? 1 : seconds);
        } else {
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        }
    }

    private void broadcastBackupNotice() {
        if (!mod().isDecicatedServer()) return;
        if (!getConfig().getBoolean(BROADCAST_NOTICE_ENABLED)) return;
        final UserMessage m;
        final String configuredMessage = getConfig().getString(BROADCAST_NOTICE_MESSAGE);
        if (configuredMessage != null) {
            m = styledRaw(configuredMessage, BROADCAST);
        } else {
            m = styledLocalized("fastback.broadcast.message", BROADCAST);
        }
        mod().sendBroadcast(m);
    }

    private boolean doNativeCheck(UserLogger ulog) {
        final GitConfig config = this.getConfig();
        if (config.getBoolean(IS_NATIVE_GIT_ENABLED)) {
            if (!EnvironmentUtils.isNativeGitInstalled()) {
                ulog.message(UserMessage.rawError("Unable to backup: native mode enabled but git is not installed."));
                return false;
            }
        }
        return true;
    }

}
