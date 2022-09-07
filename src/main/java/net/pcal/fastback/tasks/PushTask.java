package net.pcal.fastback.tasks;

import net.pcal.fastback.BranchNameUtils;
import net.pcal.fastback.GitUtils;
import net.pcal.fastback.Loggr;
import net.pcal.fastback.WorldConfig;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.ContentMergeStrategy;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.BranchNameUtils.getLastPushedBranchName;

public class PushTask extends Task {

    private final Path worldSaveDir;
    private final String branchNameToPush;
    private final TaskListener listener;
    private final Loggr logger;

    public PushTask(final Path worldSaveDir,
                    final String branchNameToPush,
                    final TaskListener listener,
                    final Loggr logger) {
        this.worldSaveDir = requireNonNull(worldSaveDir);
        this.branchNameToPush = requireNonNull(branchNameToPush);
        this.listener = requireNonNull(listener);
        this.logger = requireNonNull(logger);
    }

    @Override
    public void run() {
        super.setStarted();
        try (final Git git = Git.open(this.worldSaveDir.toFile())) {
            final WorldConfig worldConfig = WorldConfig.load(worldSaveDir, git.getRepository().getConfig());
            final String pushUrl = worldConfig.getRemotePushUri();
            if (pushUrl == null) {
                final String msg = "Skipping remote backup because no remote url has been configured.";
                this.logger.warn(msg);
                this.listener.error(msg);
                super.setFailed();
                return;
            }
            if (worldConfig.isUuidCheckEnabled()) {
                final boolean uuidCheckResult;
                try {
                    uuidCheckResult = doUuidCheck(git, worldConfig, logger);
                } catch (final GitAPIException | IOException e) {
                    logger.error("Skipping remote backup due to failed uuid check", e);
                    listener.internalError();
                    super.setFailed();
                    return;
                }
                if (!uuidCheckResult) {
                    final String msg = "Skipping remote backup due to world mismatch.";
                    logger.error(msg);
                    listener.error(msg);
                    return;
                }
            }
            final String msg = "Starting remote backup to " + pushUrl;
            logger.info(msg);
            listener.feedback(msg);
            if (worldConfig.isSmartPushEnabled()) {
                doSmartPush(git, branchNameToPush,worldConfig, logger);
            } else {
                doNaivePush(git, branchNameToPush, worldConfig, logger);
            }
            super.setCompleted();
            final Duration duration = super.getDuration();
            logger.info("Remote backup complete.  Elapsed time: " + duration.toMinutesPart() + "m " + duration.toSecondsPart() + "s");
        } catch (GitAPIException | IOException e) {
            logger.error("Remote backup failed unexpectedly", e);
            listener.internalError();
            super.setFailed();
        }
    }


    private static void doSmartPush(final Git git, final String branchNameToPush, final WorldConfig worldConfig, final Loggr logger) throws GitAPIException, IOException {
        final String remoteName = worldConfig.getRemoteName();
        final boolean tempBranchCleanup = worldConfig.isTempBranchCleanupEnabled();
        final boolean remoteTempBranchCleanup = worldConfig.isFileRemoteTempBranchCleanupEnabled();
        final String lastPushedBranchName = getLastPushedBranchName(worldConfig.worldUuid());
        if (!GitUtils.isBranchExtant(git, lastPushedBranchName, logger)) {
            logger.warn("** This appears to be the first time this world has been pushed.");
            logger.warn("** If the world is large, this may take some time.");
            git.push().setRemote(remoteName).call();
        } else {
            logger.debug("checkout");
            final String tempBranchName = BranchNameUtils.getTempBranchName(branchNameToPush);
            logger.debug("checkout temp");
            git.checkout().setCreateBranch(true).setName(tempBranchName).call();
            final ObjectId branchId = git.getRepository().resolve(lastPushedBranchName);
            logger.debug("merge");
            git.merge().setContentMergeStrategy(ContentMergeStrategy.OURS).
                    include(branchId).setMessage("Merge " + branchNameToPush + " into " + tempBranchName).call();
            logger.debug("push " + tempBranchName);
            git.push().setRemote(remoteName).setRefSpecs(new RefSpec(tempBranchName + ":" + tempBranchName), new RefSpec(branchNameToPush + ":" + branchNameToPush)).call();
            logger.debug("checkout restore");
            git.checkout().setName(branchNameToPush).call();
            if (tempBranchCleanup) {
                logger.debug("deleting local temp branch " + tempBranchName);
                git.branchDelete().setForce(true).setBranchNames(tempBranchName).call();
            }
            if (remoteTempBranchCleanup) {
                final String remoteTempBranch = "refs/heads/" + tempBranchName;
                logger.debug("deleting remote temp branch " + remoteTempBranch);
                final RefSpec deleteRemoteBranchSpec = new RefSpec().setSource(null).setDestination(remoteTempBranch);
                git.push().setRemote(remoteName).setRefSpecs(deleteRemoteBranchSpec).call();
            }
            logger.debug("push complete");
        }
        logger.debug("Updating " + lastPushedBranchName);
        git.branchCreate().setForce(true).setName(lastPushedBranchName).call();
    }

    private static void doNaivePush(final Git git, final String branchNameToPush, final WorldConfig config, final Loggr logger) throws IOException, GitAPIException {
        final String remoteName = config.getRemoteName();
        logger.debug("doing naive push");
        git.push().setRemote(remoteName).setRefSpecs(new RefSpec(branchNameToPush + ":" + branchNameToPush)).call();
        logger.debug(() -> "checking out " + branchNameToPush);
    }

    /**
     * private static void configureRemoteUpstream(final ModConfig modConfig, final Path worldSaveDir) throws IOException, GitAPIException, URISyntaxException {
     * final String remoteName = requireNonNull(modConfig.get(PUSH_REMOTE_NAME));
     * final String remoteUrl = modConfig.get(REMOTE_UPSTREAM_URI);
     * if (remoteUrl == null || remoteUrl.isEmpty()) {
     * throw new IOException("Remote push is enabled but " + REMOTE_UPSTREAM_URI + " is not configured.");
     * }
     * final URIish remoteUri = new URIish().setRawPath(remoteUrl);
     * setRemote(remoteName, remoteUri, worldSaveDir);
     * }
     * <p>
     * private static void configureFileUpstream(final ModConfig config, final Path worldSaveDir, Loggr logger) throws IOException, GitAPIException {
     * // ensure a git repository exists to push at the configured path
     * final String uuid = getWorldUuid(worldSaveDir);
     * final Path fupHome = Path.of(config.get(FILE_UPSTREAM_PATH)).resolve(uuid);
     * final Path fupGitDir = fupHome.resolve("git");
     * mkdirs(fupGitDir);
     * Git git = Git.init().setBare(config.getBoolean(FILE_UPSTREAM_BARE)).setDirectory(fupGitDir.toFile()).call();
     * final String rawConfig = config.get(FILE_UPSTREAM_GIT_CONFIG).replace(';', '\n');
     * logger.debug("updating upstream git config");
     * GitUtils.mergeGitConfig(git, rawConfig, logger);
     * // configure the world save git dir the push to it
     * final String remoteName = config.get(PUSH_REMOTE_NAME);
     * final URIish fileUri = new URIish().setScheme("file").setPath(fupGitDir.toString());
     * setRemote(remoteName, fileUri, worldSaveDir);
     * }
     **/
    private static void setRemote(String remoteName, URIish uri, final Path worldSaveDir) throws IOException, GitAPIException {
        final Git worldGit = Git.open(worldSaveDir.toFile());
        worldGit.remoteRemove().setRemoteName(remoteName).call();
        worldGit.remoteAdd().setName(remoteName).setUri(uri).call();
    }

    private static boolean doUuidCheck(Git git, WorldConfig config, Loggr logger) throws GitAPIException, IOException {
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
                logger.error("Remote at " + remoteUri + " is a backup target for a different world.");
                logger.error("Please configure a new remote for backing up this world.");
                logger.error("local: " + localUuid + ", remote: " + remoteWorldUuids);
                return false;
            }
        }
        logger.debug("world-uuid check passed.");
        return true;
    }

    //FIXME move this
    public static Set<String> getWorldUuidsOnRemote(Git worldGit, String remoteName, Loggr logger) throws GitAPIException {
        final Set<String> remoteBranchNames = GitUtils.getRemoteBranchNames(worldGit, remoteName, logger);
        final Set<String> out = new HashSet<>();
        for (String branchName : remoteBranchNames) {
            final String worldUuid = BranchNameUtils.extractWorldUuid(branchName, logger);
            if (worldUuid != null) out.add(worldUuid);
        }
        return out;
    }

}
