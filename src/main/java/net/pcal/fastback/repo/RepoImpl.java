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
import net.pcal.fastback.ModContext;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.Message;
import net.pcal.fastback.utils.FileUtils;
import net.pcal.fastback.utils.NativeGitUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RefSpec;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.config.GitConfigKey.IS_NATIVE_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.REMOTE_NAME;
import static net.pcal.fastback.config.GitConfigKey.UPDATE_GITATTRIBUTES_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.UPDATE_GITIGNORE_ENABLED;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.utils.FileUtils.writeResourceToFile;

class RepoImpl implements Repo {

    static final Path WORLD_UUID_PATH = Path.of("fastback/world.uuid");

    private final Git jgit;
    private final ModContext ctx;
    private final Logger log;

    RepoImpl(final Git git,
             final ModContext ctx,
             final Logger logger) {
        this.jgit = requireNonNull(git);
        this.ctx = requireNonNull(ctx);
        this.log = requireNonNull(logger);
    }

    @Override
    public void doCommitAndPush() throws IOException {
        if (!doNativeCheck()) return;
        final SnapshotId newSid = CommitUtils.doCommitSnapshot(this, ctx, log);
        PushUtils.doPush(newSid, this, log);
        log.chat(localized("fastback.chat.backup-complete"));
    }

    @Override
    public void doCommitSnapshot() throws IOException {
        if (!doNativeCheck()) return;
        CommitUtils.doCommitSnapshot(this, ctx, log);
        log.chat(localized("fastback.chat.backup-complete"));
    }

    @Override
    public Callable<Collection<SnapshotId>> createLocalPruneTask() {
        return new JGitLocalPruneTask(this,  log);
    }

    @Override
    public Callable<Void> createGcTask() {
        return new JGitGcTask(this, ctx, log);
    }

    @Override
    public Callable<Collection<SnapshotId>> createRemotePruneTask() {
        return new RemotePruneTask(this, log);
    }

    @Override
    public Callable<Path> restoreSnapshotTask(String uri, Path restoresDir, String worldName, SnapshotId sid, Logger log) throws IOException {
        return new JGitRestoreSnapshotTask(uri, restoresDir, worldName, sid, log);
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
            return new ListSnapshotsTask(refProvider, this.log).call();
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
            return new ListSnapshotsTask(refProvider, log).call();
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }

    @Override
    public GitConfig getConfig() {
        return GitConfig.load(this.jgit);
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
    public void deleteRemoteBranch(String remoteName, String remoteBranchName) throws IOException {
        RefSpec refSpec = new RefSpec()
                .setSource(null)
                .setDestination("refs/heads/" + remoteBranchName);
        try {
            this.jgit.push().setRefSpecs(refSpec).setRemote(remoteName).call();
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void deleteBranch(String branchName) throws GitAPIException {
        this.jgit.branchDelete().setForce(true).setBranchNames(branchName).call();
    }

    Git getJGit() {
        return this.jgit;
    }

    @Override
    public void close() {
        this.getJGit().close();
    }

    @Override
    public void doWorldMaintenance(final Logger logger) throws IOException, IOException {
        logger.info("Doing world maintenance");
        final Path worldSaveDir = jgit.getRepository().getWorkTree().toPath();
        ensureWorldHasUuid(worldSaveDir, logger);
        final GitConfig config = GitConfig.load(jgit);
        if (config.getBoolean(UPDATE_GITIGNORE_ENABLED)) {
            final Path targetPath = worldSaveDir.resolve(".gitignore");
            writeResourceToFile("world/gitignore", targetPath);
        }
        if (config.getBoolean(UPDATE_GITATTRIBUTES_ENABLED)) {
            final Path targetPath = worldSaveDir.resolve(".gitattributes");
            if (config.getBoolean(IS_NATIVE_ENABLED)) {
                writeResourceToFile("world/gitattributes-native", targetPath);
            } else {
                writeResourceToFile("world/gitattributes-jgit", targetPath);
            }
        }
    }

    private boolean doNativeCheck() {
        final GitConfig config = this.getConfig();
        if (config.getBoolean(IS_NATIVE_ENABLED)) {
            if (!NativeGitUtils.isNativeGitInstalled(this.log)) {
                log.chat(Message.rawError("Unable to backup: native mode enabled but git is not installed."));
                return false;
            }
        }
        return true;
    }

    private static void ensureWorldHasUuid(final Path worldSaveDir, final Logger logger) throws IOException {
        final Path worldUuidpath = worldSaveDir.resolve(WORLD_UUID_PATH);
        if (!worldUuidpath.toFile().exists()) {
            FileUtils.mkdirs(worldUuidpath.getParent());
            final String newUuid = UUID.randomUUID().toString();
            try (final FileWriter fw = new FileWriter(worldUuidpath.toFile())) {
                fw.append(newUuid);
                fw.append('\n');
            }
            logger.info("Generated new world.uuid " + newUuid);
        }
    }
}