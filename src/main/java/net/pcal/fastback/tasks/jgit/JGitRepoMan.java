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

package net.pcal.fastback.tasks.jgit;

import com.google.common.collect.ListMultimap;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.config.RepoConfigUtils;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.tasks.RepoMan;
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

public class JGitRepoMan implements RepoMan {

    private final Git repo;
    private final ModContext ctx;
    private final Logger log;

    public JGitRepoMan(final Git git,
                       final ModContext ctx,
                       final Logger logger) {
        this.repo = requireNonNull(git);
        this.ctx = requireNonNull(ctx);
        this.log = requireNonNull(logger);

    }
    @Override
    public Callable<Void> createCommitAndPushTask() {
        return new CommitAndPushTask(repo, ctx, log);
    }

    @Override
    public Callable<SnapshotId> createCommitTask() {
        return new CommitTask(repo, ctx, log);
    }

    @Override
    public Callable<Collection<SnapshotId>> createLocalPruneTask() {
        return new LocalPruneTask(this, ctx, log);
    }

    @Override
    public Callable<Void> createGcTask() {
        return new GcTask(this, ctx, log);
    }

    @Override
    public String getWorldUuid() throws IOException {
        return RepoConfigUtils.getWorldUuid(this.repo);
    }

    @Override
    public ListMultimap<String, SnapshotId> listSnapshots() throws GitAPIException, IOException {
        final JGitSupplier<Collection<Ref>> refProvider = ()->  repo.branchList().call();
        return new ListSnapshotsTask(refProvider, this.log).call();
    }

    @Override
    public ListMultimap<String, SnapshotId> listRemoteSnapshots() throws GitAPIException, IOException {
        final GitConfig conf = GitConfig.load(repo);
        final String remoteName = conf.getString(REMOTE_NAME);
        final JGitSupplier<Collection<Ref>> refProvider = ()-> repo.lsRemote().setRemote(remoteName).setHeads(true).call();
        return new ListSnapshotsTask(refProvider, log).call();
    }

    @Override
    public GitConfig getConfig() {
        return GitConfig.load(this.repo);
    }

    @Override
    public File getDirectory() throws NoWorkTreeException {
        return this.repo.getRepository().getDirectory();
    }

    @Override
    public File getWorkTree() throws NoWorkTreeException {
        return this.repo.getRepository().getWorkTree();
    }

    @Override
    public void deleteRemoteBranch(String remoteName, String remoteBranchName) throws GitAPIException {
        RefSpec refSpec = new RefSpec()
                .setSource(null)
                .setDestination("refs/heads/" + remoteBranchName);
        this.repo.push().setRefSpecs(refSpec).setRemote(remoteName).call();
    }

    @Override
    public void deleteBranch(String branchName) throws GitAPIException {
        this.repo.branchDelete().setForce(true).setBranchNames(branchName).call();
    }

    Git getJGit() {
        return this.repo;
    }

    @Override
    public void close() {
        this.getJGit().close();
    }
}
