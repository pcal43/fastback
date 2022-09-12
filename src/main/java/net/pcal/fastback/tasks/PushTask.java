package net.pcal.fastback.tasks;

import net.pcal.fastback.BranchNameUtils;
import net.pcal.fastback.GitUtils;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.IncrementalProgressMonitor;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.LoggingProgressMonitor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.merge.ContentMergeStrategy;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.BranchNameUtils.getLastPushedBranchName;

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
            if (worldConfig.isUuidCheckEnabled()) {
                final boolean uuidCheckResult;
                try {
                    uuidCheckResult = doUuidCheck(git, worldConfig, logger);
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
                doSmartPush(git, branchNameToPush, worldConfig, logger);
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

    private static void doSmartPush(final Git git, final String branchNameToPush, final WorldConfig worldConfig, final Logger logger) throws GitAPIException, IOException {
        final String remoteName = worldConfig.getRemoteName();
        final String lastPushedBranchName = getLastPushedBranchName(worldConfig.worldUuid());

        final ProgressMonitor pm = new IncrementalProgressMonitor(new LoggingProgressMonitor(logger), 100);

        if (!GitUtils.isBranchExtant(git, lastPushedBranchName, logger)) {
            logger.warn("** This appears to be the first time this world has been pushed.");
            logger.warn("** If the world is large, this may take some time.");
            git.push().setProgressMonitor(pm).setRemote(remoteName).call();
        } else {
            logger.debug("checkout");
            final String tempBranchName = BranchNameUtils.getTempBranchName(branchNameToPush);
            logger.debug("checkout temp");
            git.checkout().setCreateBranch(true).setName(tempBranchName).call();
            final ObjectId branchId = git.getRepository().resolve(lastPushedBranchName);
            logger.debug("merge");
            git.merge().setContentMergeStrategy(ContentMergeStrategy.OURS).
                    include(branchId).setMessage("Merge " + branchNameToPush + " into " + tempBranchName).call();
            logger.debug("checking out " + branchNameToPush);
            git.checkout().setName(branchNameToPush).call();
            logger.debug("push " + tempBranchName);
            git.push().setProgressMonitor(pm).setRemote(remoteName).setRefSpecs(new RefSpec(tempBranchName + ":" + tempBranchName), new RefSpec(branchNameToPush + ":" + branchNameToPush)).call();
            logger.debug("checkout restore");
            logger.notify("Cleaning up");
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
        logger.debug("Updating " + lastPushedBranchName);
        git.branchCreate().setForce(true).setName(lastPushedBranchName).call();
    }

    private static void doNaivePush(final Git git, final String branchNameToPush, final WorldConfig config, final Logger logger) throws IOException, GitAPIException {
        final String remoteName = config.getRemoteName();
        logger.debug("doing naive push");
        git.push().setRemote(remoteName).setRefSpecs(new RefSpec(branchNameToPush + ":" + branchNameToPush)).call();
        logger.debug("checking out " + branchNameToPush);
    }

    private static boolean doUuidCheck(Git git, WorldConfig config, Logger logger) throws GitAPIException, IOException {
        final Set<String> remoteWorldUuids = getWorldUuidsOnRemote(git, config.getRemoteName(), logger);
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

    private static Set<String> getWorldUuidsOnRemote(Git worldGit, String remoteName, Logger logger) throws GitAPIException {
        final Set<String> remoteBranchNames = GitUtils.getRemoteBranchNames(worldGit, remoteName, logger);
        final Set<String> out = new HashSet<>();
        for (String branchName : remoteBranchNames) {
            final String worldUuid = BranchNameUtils.extractWorldUuid(branchName, logger);
            if (worldUuid != null) out.add(worldUuid);
        }
        return out;
    }

}
