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

import com.google.common.collect.ListMultimap;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.progress.IncrementalProgressMonitor;
import net.pcal.fastback.progress.PercentageProgressMonitor;
import net.pcal.fastback.utils.GitUtils;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.ContentMergeStrategy;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.Message.localized;

public class PushTask implements Callable<Void> {

    private final ModContext ctx;
    private final Logger log;
    private final Git git;
    private final SnapshotId sid;

    public PushTask(final Git git,
                    final ModContext ctx,
                    final Logger log,
                    final SnapshotId sid) {
        this.git = requireNonNull(git);
        this.ctx = requireNonNull(ctx);
        this.log = requireNonNull(log);
        this.sid = requireNonNull(sid);
    }

    @Override
    public Void call() throws GitAPIException, IOException {
        this.log.hud(localized("fastback.hud.remote-uploading", 0));
        final WorldConfig worldConfig = WorldConfig.load(git);
        final String pushUrl = worldConfig.getRemotePushUrl();
        if (pushUrl == null) {
            final String msg = "Skipping remote backup because no remote url has been configured.";
            this.log.warn(msg);
            return null;
        }
        final Collection<Ref> remoteBranchRefs = git.lsRemote().setHeads(true).setTags(false).
                setRemote(worldConfig.getRemoteName()).call();
        final ListMultimap<String, SnapshotId> snapshotsPerWorld =
                SnapshotId.getSnapshotsPerWorld(remoteBranchRefs, log);
        if (worldConfig.isUuidCheckEnabled()) {
            final boolean uuidCheckResult;
            try {
                uuidCheckResult = doUuidCheck(git, snapshotsPerWorld.keySet(), worldConfig, log);
            } catch (final GitAPIException | IOException e) {
                log.internalError("Skipping remote backup due to failed uuid check", e);
                return null;
            }
            if (!uuidCheckResult) {
                log.warn("Skipping remote backup due to world mismatch.");
                return null;
            }
        }
        log.info("Pushing to " + worldConfig.getRemotePushUrl());
        if (worldConfig.isSmartPushEnabled()) {
            doSmartPush(git, snapshotsPerWorld.get(worldConfig.worldUuid()), this.sid.getBranchName(), worldConfig, log);
        } else {
            doNaivePush(git, this.sid.getBranchName(), worldConfig, log);
        }
        log.info("Remote backup complete.");
        return null;
    }

    private static void doSmartPush(final Git git, List<SnapshotId> remoteSnapshots, final String branchNameToPush, final WorldConfig worldConfig, final Logger logger) throws GitAPIException, IOException {
        final String remoteName = worldConfig.getRemoteName();
        final String worldUuid = worldConfig.worldUuid();
        final SnapshotId latestCommonSnapshot;
        if (remoteSnapshots.isEmpty()) {
            logger.warn("** This appears to be the first time this world has been pushed.");
            logger.warn("** If the world is large, this may take some time.");
            doNaivePush(git, branchNameToPush, worldConfig, logger);
            return;
        } else {
            final Collection<Ref> localBranchRefs = git.branchList().call();
            final ListMultimap<String, SnapshotId> localSnapshotsPerWorld =
                    SnapshotId.getSnapshotsPerWorld(localBranchRefs, logger);
            final List<SnapshotId> localSnapshots = localSnapshotsPerWorld.get(worldUuid);
            remoteSnapshots.retainAll(localSnapshots);
            if (remoteSnapshots.isEmpty()) {
                logger.warn("No common snapshots found between local and remote.");
                logger.warn("Doing a full push.  This may take some time.");
                doNaivePush(git, branchNameToPush, worldConfig, logger);
                return;
            } else {
                Collections.sort(remoteSnapshots);
                latestCommonSnapshot = remoteSnapshots.get(remoteSnapshots.size() - 1);
                logger.info("Using existing snapshot " + latestCommonSnapshot + " for common history");
            }
        }
        // ok, we have a common snapshot that we can use to create a fake merge history.
        final String tempBranchName = getTempBranchName(branchNameToPush);
        logger.debug("Creating out temp branch " + tempBranchName);
        git.checkout().setUpstreamMode(SetupUpstreamMode.NOTRACK).setCreateBranch(true).setName(tempBranchName).call();
        final ObjectId branchId = git.getRepository().resolve(latestCommonSnapshot.getBranchName());
        logger.debug("Merging " + latestCommonSnapshot.getBranchName());
        git.merge().setContentMergeStrategy(ContentMergeStrategy.OURS).
                include(branchId).setMessage("Merge " + branchId + " into " + tempBranchName).call();
        logger.debug("Checking out " + branchNameToPush);
        git.checkout().setUpstreamMode(SetupUpstreamMode.NOTRACK).setName(branchNameToPush).call();
        logger.debug("Pushing " + tempBranchName);
        final ProgressMonitor pm = new IncrementalProgressMonitor(new PushProgressMonitor(logger), 100);
        git.push().setProgressMonitor(pm).setRemote(remoteName).
                setRefSpecs(new RefSpec(tempBranchName + ":" + tempBranchName),
                        new RefSpec(branchNameToPush + ":" + branchNameToPush)).call();
        logger.info("Cleaning up branches");
        if (worldConfig.isTempBranchCleanupEnabled()) {
            logger.debug("deleting local temp branch " + tempBranchName);
            git.branchDelete().setForce(true).setBranchNames(tempBranchName).call();
        }
        if (worldConfig.isRemoteTempBranchCleanupEnabled()) {
            final String remoteTempBranch = "refs/heads/" + tempBranchName;
            logger.debug("deleting remote temp branch " + remoteTempBranch);
            final RefSpec deleteRemoteBranchSpec = new RefSpec().setSource(null).setDestination(remoteTempBranch);

            git.push().setProgressMonitor(pm).setRemote(remoteName).setRefSpecs(deleteRemoteBranchSpec).call();
        }
        logger.debug("push complete");
    }

    private static void doNaivePush(final Git git, final String branchNameToPush, final WorldConfig config, final Logger logger) throws IOException, GitAPIException {
        final ProgressMonitor pm = new IncrementalProgressMonitor(new PushProgressMonitor(logger), 100);
        final String remoteName = config.getRemoteName();
        logger.info("Doing naive push of " + branchNameToPush);
        Iterable<PushResult> result = git.push().setProgressMonitor(pm).setRemote(remoteName).
                setRefSpecs(new RefSpec(branchNameToPush + ":" + branchNameToPush)).call();
        for(PushResult pr : result) {
            System.out.println("RESULT: "+pr.getTrackingRefUpdates().toString());
        }
    }

    private static boolean doUuidCheck(Git git, Set<String> remoteWorldUuids, WorldConfig config, Logger logger) throws GitAPIException, IOException {
        final String localUuid = config.worldUuid();
        if (remoteWorldUuids.size() > 2) {
            logger.warn("Remote has more than one world-uuid.  This is unusual. " + remoteWorldUuids);
        }
        if (remoteWorldUuids.isEmpty()) {
            if (!remoteWorldUuids.contains(localUuid)) {
                final URIish remoteUri = GitUtils.getRemoteUri(git, config.getRemoteName(), logger);
                logger.chatError(localized("fastback.chat.push-uuid-mismatch", remoteUri));
                logger.info("local: " + localUuid + ", remote: " + remoteWorldUuids);
                return false;
            }
        }
        logger.debug("world-uuid check passed.");
        return true;
    }

    static boolean isTempBranch(String branchName) {
        return branchName.startsWith("temp/");
    }

    private static String getTempBranchName(String uniqueName) {
        return "temp/" + uniqueName;
    }

    private static class PushProgressMonitor extends PercentageProgressMonitor {

        private final Logger logger;

        public PushProgressMonitor(Logger logger) {
            this.logger = requireNonNull(logger);
        }

        @Override
        public void progressStart(String task) {
            this.logger.info(task);
        }

        @Override
        public void progressUpdate(String task, int percentage) {
            this.logger.info(task + " " + percentage + "%");
            if (task.contains("Finding sources")) {
                this.logger.hud(localized("fastback.hud.remote-preparing", percentage/2));
            } else if (task.contains("Writing objects")) {
                this.logger.hud(localized("fastback.hud.remote-uploading", 50 +(percentage/2)));
            }
        }

        @Override
        public void progressDone(String task) {
            logger.info("Done "+task);
        }
    }
}
