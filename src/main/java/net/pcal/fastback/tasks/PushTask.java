package net.pcal.fastback.tasks;

import com.google.common.collect.ListMultimap;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.IncrementalProgressMonitor;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.LoggingProgressMonitor;
import net.pcal.fastback.utils.GitUtils;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.ContentMergeStrategy;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static net.minecraft.text.Text.literal;

public class PushTask extends Task {

    private final Path worldSaveDir;
    private final String branchNameToPush;
    private final Logger logger;

    public PushTask(final Path worldSaveDir,
                    final String branchNameToPush,
                    final Logger logger) {
        this.worldSaveDir = requireNonNull(worldSaveDir);
        this.branchNameToPush = requireNonNull(branchNameToPush);
        this.logger = requireNonNull(logger);
    }

    private static String getTempBranchName(String uniqueName) {
        return "temp/" + uniqueName;
    }

    @Override
    public void run() {
        super.setStarted();
        try (final Git git = Git.open(this.worldSaveDir.toFile())) {
            final WorldConfig worldConfig = WorldConfig.load(worldSaveDir, git.getRepository().getConfig());
            final String pushUrl = worldConfig.getRemotePushUrl();
            if (pushUrl == null) {
                final String msg = "Skipping remote backup because no remote url has been configured.";
                this.logger.warn(msg);
                super.setFailed();
                return;
            }
            final Collection<Ref> remoteBranchRefs = git.lsRemote().setHeads(true).setTags(false).
                    setRemote(worldConfig.getRemoteName()).call();
            final ListMultimap<String, SnapshotId> snapshotsPerWorld =
                    SnapshotId.getSnapshotsPerWorld(remoteBranchRefs, logger);
            if (worldConfig.isUuidCheckEnabled()) {
                final boolean uuidCheckResult;
                try {
                    uuidCheckResult = doUuidCheck(git, snapshotsPerWorld.keySet(), worldConfig, logger);
                } catch (final GitAPIException | IOException e) {
                    logger.internalError("Skipping remote backup due to failed uuid check", e);
                    super.setFailed();
                    return;
                }
                if (!uuidCheckResult) {
                    final String msg = "Skipping remote backup due to world mismatch.";
                    logger.notifyError(msg);
                    super.setFailed();
                    return;
                }
            }
            logger.info("Pushing to " + worldConfig.getRemotePushUrl());
            if (worldConfig.isSmartPushEnabled()) {
                doSmartPush(git, snapshotsPerWorld.get(worldConfig.worldUuid()), branchNameToPush, worldConfig, logger);
            } else {
                doNaivePush(git, branchNameToPush, worldConfig, logger);
            }
            super.setCompleted();
            final Duration duration = super.getDuration();
            logger.info("Remote backup complete.  Elapsed time: " + duration.toMinutesPart() + "m " + duration.toSecondsPart() + "s");
        } catch (GitAPIException | IOException e) {
            logger.internalError("Remote backup failed unexpectedly", e);
            super.setFailed();
        }
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
            remoteSnapshots.removeAll(localSnapshots);
            if (remoteSnapshots.isEmpty()) {
                logger.warn("No common snapshots found between local and remote.");
                logger.warn("Doing a full push.  This may take some time.");
                doNaivePush(git, branchNameToPush, worldConfig, logger);
                return;
            } else {
                Collections.sort(remoteSnapshots);
                latestCommonSnapshot = remoteSnapshots.get(0);
                logger.info("Using existing snapshot " + latestCommonSnapshot + " for common history");
            }
        }

        final String tempBranchName = getTempBranchName(branchNameToPush);
        logger.debug("Creating out temp branch " + tempBranchName);
        git.checkout().setCreateBranch(true).setName(tempBranchName).call();
        final ObjectId branchId = git.getRepository().resolve(latestCommonSnapshot.getBranchName());
        logger.debug("Merging " + latestCommonSnapshot.getBranchName());
        git.merge().setContentMergeStrategy(ContentMergeStrategy.OURS).
                include(branchId).setMessage("Merge " + branchId + " into " + tempBranchName).call();
        logger.debug("Checking out " + branchNameToPush);
        git.checkout().setName(branchNameToPush).call();
        logger.debug("Pushing " + tempBranchName);
        final ProgressMonitor pm = new IncrementalProgressMonitor(new LoggingProgressMonitor(logger), 100);
        git.push().setProgressMonitor(pm).setRemote(remoteName).
                setRefSpecs(new RefSpec(tempBranchName + ":" + tempBranchName),
                        new RefSpec(branchNameToPush + ":" + branchNameToPush)).call();
        logger.debug("checkout restore");
        logger.notify(literal("Cleaning up"));
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
        final ProgressMonitor pm = new IncrementalProgressMonitor(new LoggingProgressMonitor(logger), 100);
        final String remoteName = config.getRemoteName();
        logger.info("Doing naive push of " + branchNameToPush);
        git.push().setProgressMonitor(pm).setRemote(remoteName).
                setRefSpecs(new RefSpec(branchNameToPush + ":" + branchNameToPush)).call();
    }

    private static boolean doUuidCheck(Git git, Set<String> remoteWorldUuids, WorldConfig config, Logger logger) throws GitAPIException, IOException {
        final String localUuid = config.worldUuid();
        if (remoteWorldUuids.size() > 2) {
            logger.warn("Remote has more than one world-uuid.  This is unusual. " + remoteWorldUuids);
        }
        if (remoteWorldUuids.isEmpty()) {
            logger.debug("Remote does not have any previously-backed up worlds.");
        } else {
            if (!remoteWorldUuids.contains(localUuid)) {
                final URIish remoteUri = GitUtils.getRemoteUri(git, config.getRemoteName(), logger);
                logger.notifyError("Remote at " + remoteUri + " is a backup target for a different world.");
                logger.notifyError("Please configure a new remote for backing up this world.");
                logger.notifyError("local: " + localUuid + ", remote: " + remoteWorldUuids);
                return false;
            }
        }
        logger.debug("world-uuid check passed.");
        return true;
    }

}
