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
import net.pcal.fastback.config.RepoConfigUtils;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RefSpec;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.config.GitConfigKey.REMOTE_NAME;

class RepoImpl implements Repo {

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
    public Callable<Void> createCommitAndPushTask() {
        return new JGitCommitAndPushTask(this, ctx, log);
    }

    @Override
    public Callable<SnapshotId> createCommitTask() {
        return new JGitCommitTask(this, ctx, log);
    }

    @Override
    public Callable<Collection<SnapshotId>> createLocalPruneTask() {
        return new JGitLocalPruneTask(this, ctx, log);
    }

    @Override
    public Callable<Void> createGcTask() {
        return new JGitGcTask(this, ctx, log);
    }

    @Override
    public Callable<Collection<SnapshotId>> createRemotePruneTask() {
        return new RemotePruneTask(this, ctx, log);
    }

    @Override
    public String getWorldUuid() throws IOException {
        return RepoConfigUtils.getWorldUuid(this.jgit);
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
}
