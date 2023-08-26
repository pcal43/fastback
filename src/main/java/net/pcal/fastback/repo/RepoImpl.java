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
import net.pcal.fastback.config.GitConfigKey;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.repo.SnapshotIdUtils.SnapshotIdCodec;
import net.pcal.fastback.repo.WorldIdUtils.WorldIdInfo;
import net.pcal.fastback.utils.EnvironmentUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.config.FastbackConfigKey.BROADCAST_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.BROADCAST_MESSAGE;
import static net.pcal.fastback.config.FastbackConfigKey.IS_LOCK_CLEANUP_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.REMOTE_NAME;
import static net.pcal.fastback.config.FastbackConfigKey.RESTORE_DIRECTORY;
import static net.pcal.fastback.config.OtherConfigKey.REMOTE_PUSH_URL;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.BROADCAST;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.WARNING;
import static net.pcal.fastback.logging.UserMessage.localized;
import static net.pcal.fastback.logging.UserMessage.styledLocalized;
import static net.pcal.fastback.logging.UserMessage.styledRaw;
import static net.pcal.fastback.mod.Mod.mod;
import static org.eclipse.jgit.util.FileUtils.RETRY;

/**
 * @author pcal
 * @since 0.13.0
 */
class RepoImpl implements Repo {

    // ======================================================================
    // Fields

    private final Git jgit;
    private GitConfig config;
    private WorldIdInfo worldIdInfo;

    // ======================================================================
    // Constructors

    RepoImpl(final Git jgit) {
        this.jgit = requireNonNull(jgit);
    }

    // ======================================================================
    // 'do' methods - implement higher-level command-oriented logic.

    @Override
    public void doCommitAndPush(final UserLogger ulog) {
        if (!doNativeCheck(ulog)) return;
        checkIndexLock(ulog);
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
        ulog.message(localized("fastback.chat.backup-complete-elapsed", newSid, getDuration(start)));
    }

    @Override
    public void doCommitSnapshot(final UserLogger ulog) {
        if (!doNativeCheck(ulog)) return;
        checkIndexLock(ulog);
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
    public void doPushSnapshot(SnapshotId sid, final UserLogger ulog) throws IOException, ParseException {
        if (!this.getConfig().isSet(REMOTE_PUSH_URL)) {
            ulog.message(styledLocalized("No remote is configured.  Run set-remote <url>", ERROR)); //FIXME i18n
        } else {
            PushUtils.doPush(sid, this, ulog);
            ulog.message(UserMessage.localized("Successfully pushed " + sid)); // FIXME i18n
        }
    }

    // ======================================================================
    // Other repo implementation

    @Override
    public WorldId getWorldId() throws IOException {
        return WorldIdUtils.getWorldIdInfo(this.getWorkTree().toPath()).wid();
    }

    @Override
    public Set<SnapshotId> getLocalSnapshots() throws IOException {
        final JGitSupplier<Collection<Ref>> refProvider = () -> {
            try {
                return jgit.branchList().call();
            } catch (GitAPIException e) {
                throw new IOException(e);
            }
        };
        try {
            return BranchUtils.listSnapshots(this, refProvider);
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Set<SnapshotId> getRemoteSnapshots() throws IOException {
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
            return BranchUtils.listSnapshots(this, refProvider);
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

    @Override
    public void close() {
        this.getJGit().close();
    }

    @Override
    public void setConfigValue(GitConfigKey key, boolean value, UserLogger userlog) {
        requireNonNull(key);
        if (key == IS_NATIVE_GIT_ENABLED) { // FIXME this is gross.  find some other place
            try {
                MaintenanceUtils.setNativeGitEnabled(value, this, userlog);
            } catch (IOException e) {
                userlog.internalError(e);
            }
        } else {
            try {
                this.getConfig().updater().set(key, value).save();
            } catch (IOException e) {
                userlog.internalError(e);
            }
        }
    }

    @Override
    public Path getRestoresDir() throws IOException {
        if (getConfig().getString(RESTORE_DIRECTORY) != null) {
            return Paths.get(getConfig().getString(RESTORE_DIRECTORY));
        } else {
            return mod().getDefaultRestoresDir();
        }
    }

    @Override
    public SnapshotId createSnapshotId(String shortName) throws IOException, ParseException {
        return getWorldIdInfo().sidCodec().create(this.getWorldId(), shortName);
    }

    // ======================================================================
    // Package-private

    SnapshotIdCodec getSidCodec() throws IOException {
        return this.getWorldIdInfo().sidCodec();
    }

    Git getJGit() {
        return this.jgit;
    }

    // ======================================================================
    // Private

    private WorldIdInfo getWorldIdInfo() throws IOException {
        if (this.worldIdInfo == null) {
            this.worldIdInfo = WorldIdUtils.getWorldIdInfo(this.getWorkTree().toPath());
        }
        return this.worldIdInfo;
    }

    private void checkIndexLock(UserLogger ulog) {
        final File lockFile = this.getWorkTree().toPath().resolve(".git/index.lock").toFile();
        if (lockFile.exists()) {
            ulog.message(styledRaw(lockFile.getAbsolutePath() + "exists", WARNING)); //FIXME i18n
            if (getConfig().getBoolean(IS_LOCK_CLEANUP_ENABLED)) {
                ulog.message(styledRaw("lock-cleanup-enabled = true, attempting to delete index.lock", WARNING)); //FIXME i18n
                try {
                    FileUtils.delete(lockFile, RETRY);
                } catch (IOException e) {
                    syslog().debug(e); // we kind of don't care
                }
                if (lockFile.exists()) {
                    ulog.message(styledRaw("Cleanup failed.  Your backup will probably not succeed.", ERROR));
                } else {
                    ulog.message(styledRaw("Cleanup succeeded, proceeding with backup.  But if you see this message again, you should check your system to see if some other git process is accessing your backup.", WARNING));
                }
            } else {
                ulog.message(styledRaw("Please check to see if other processes are using this git repo.  If you're sure they aren't, you can enable automatic index.lock cleanup by typing '/set lock-cleanup enabled'", WARNING));
                ulog.message(styledRaw("Proceeding with backup but it will probably not succeed.", WARNING));

            }
        }
    }

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
        if (!getConfig().getBoolean(BROADCAST_ENABLED)) return;
        final UserMessage m;
        final String configuredMessage = getConfig().getString(BROADCAST_MESSAGE);
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
