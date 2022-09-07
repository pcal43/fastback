package net.pcal.fastback;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.ContentMergeStrategy;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import static java.nio.file.StandardCopyOption.*;
import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.FileUtils.mkdirs;
import static net.pcal.fastback.WorldUtils.WORLD_INFO_PATH;
import static net.pcal.fastback.WorldUtils.getWorldUuid;
import static net.pcal.fastback.LogUtils.debug;
import static net.pcal.fastback.LogUtils.error;
import static net.pcal.fastback.LogUtils.info;
import static net.pcal.fastback.LogUtils.warn;
import static net.pcal.fastback.ModConfig.Key.*;

public class PushUtils {

    public static boolean pushIfNecessary(final String branchNameToPush, final ModConfig modConfig, final Path worldSaveDir, final Loginator logger) throws IOException, GitAPIException {
        requireNonNull(branchNameToPush);
        requireNonNull(modConfig);
        requireNonNull(worldSaveDir);
        requireNonNull(logger);

        final long startTime = System.currentTimeMillis();
        final boolean isPushEnabled = modConfig.getBoolean(PUSH_ENABLED);
        if (!isPushEnabled) {
            info(logger, "Remote backup disabled in config.");
            return false;
        }
        final boolean isFupEnabled = modConfig.getBoolean(FILE_UPSTREAM_ENABLED);
        final boolean isRupEnabled = modConfig.getBoolean(REMOTE_UPSTREAM_ENABLED);
        final String remoteName = modConfig.get(PUSH_REMOTE_NAME);
        final Git git = Git.open(worldSaveDir.toFile());

        if (isFupEnabled) {
            if (isRupEnabled) {
                error(logger, "Skipping remote backup.  " +
                        FILE_UPSTREAM_ENABLED.getPropertyName() + " and " +
                        REMOTE_UPSTREAM_ENABLED.getPropertyName() + " cannot both be set to true.");
                return false;
            }
            configureFileUpstream(modConfig, worldSaveDir, logger);
        } else if (isRupEnabled) {
            try {
                configureRemoteUpstream(modConfig, worldSaveDir);
            } catch (URISyntaxException e) {
                error(logger, "Skipping remote backup because an invalid URI is configured for " +
                        REMOTE_UPSTREAM_URI.getPropertyName(), e);
                return false;
            }
        } else {
            if (GitUtils.getRemoteUri(git, remoteName, logger) == null) {
                error(logger, "Skipping remote backup.  Remote '" + remoteName +
                        "' is not set and no auto-remote configuration is enabled.");
                return false;
            } else {
                info(logger, "Using manually-configured remote '" + remoteName + "'");
            }
        }
        final URIish pushUri = GitUtils.getRemoteUri(git, remoteName, logger);
        if (pushUri == null) {
            throw new IOException("No remote '" + remoteName + "' is configured.");
        }
        if (!doUuidCheck(modConfig, git, remoteName, worldSaveDir, logger)) {
            warn(logger, "Skipping remote backup due to world mismatch.");
            return false;
        }
        info(logger, "Starting remote backup to " + pushUri);
        if (modConfig.getBoolean(SMART_PUSH_ENABLED)) {
            doSmartPush(git, branchNameToPush, modConfig, logger);
        } else {
            doNaivePush(git, branchNameToPush, modConfig, logger);
        }
        final Duration duration = Duration.ofMillis(System.currentTimeMillis() - startTime);
        info(logger, "Remote backup complete.  Elapsed time: " + duration.toMinutesPart() + "m " + duration.toSecondsPart() + "s");
        return true;
    }

    private static void doSmartPush(final Git git, final String branchNameToPush, final ModConfig modConfig, final Loginator logger) throws GitAPIException, IOException {
        final String remoteName = modConfig.get(PUSH_REMOTE_NAME);
        final String lastPushedBranchName = modConfig.get(SMART_PUSH_BRANCH_NAME);
        final String tempBranchNameFormat = modConfig.get(SMART_PUSH_TEMP_BRANCH_FORMAT);
        final boolean tempBranchCleanup = modConfig.getBoolean(SMART_PUSH_TEMP_BRANCH_CLEANUP_ENABLED);
        final boolean remoteTempBranchCleanup = modConfig.getBoolean(SMART_PUSH_REMOTE_TEMP_BRANCH_CLEANUP_ENABLED);
        if (!GitUtils.isBranchExtant(git, lastPushedBranchName, logger)) {
            warn(logger, "** This appears to be the first time this world has been pushed.");
            warn(logger, "** If the world is large, this may take some time.");
            git.push().setRemote(remoteName).call();
        } else {
            debug(logger, "checkout");
            final String tempBranchName = tempBranchNameFormat.formatted(branchNameToPush);
            debug(logger, "checkout temp");
            git.checkout().setCreateBranch(true).setName(tempBranchName).call();
            final ObjectId branchId = git.getRepository().resolve(lastPushedBranchName);
            debug(logger, "merge");
            git.merge().setContentMergeStrategy(ContentMergeStrategy.OURS).
                    include(branchId).setMessage("Merge " + branchNameToPush + " into " + tempBranchName).call();
            debug(logger, "push " + tempBranchName);
            git.push().setRemote(remoteName).setRefSpecs(new RefSpec(tempBranchName + ":" + tempBranchName), new RefSpec(branchNameToPush + ":" + branchNameToPush)).call();
            debug(logger, "checkout restore");
            git.checkout().setName(branchNameToPush).call();
            if (tempBranchCleanup) {
                debug(logger, "deleting local temp branch " + tempBranchName);
                git.branchDelete().setForce(true).setBranchNames(tempBranchName).call();
            }
            if (remoteTempBranchCleanup) {
                final String remoteTempBranch = "refs/heads/" + tempBranchName;
                debug(logger, "deleting remote temp branch " + remoteTempBranch);
                final RefSpec deleteRemoteBranchSpec = new RefSpec().setSource(null).setDestination(remoteTempBranch);
                git.push().setRemote(remoteName).setRefSpecs(deleteRemoteBranchSpec).call();
            }
            debug(logger, "push complete");
        }
        debug(logger, "Updating " + lastPushedBranchName);
        git.branchCreate().setForce(true).setName(lastPushedBranchName).call();
    }

    private static void doNaivePush(final Git git, final String branchNameToPush, final ModConfig modConfig, final Loginator logger) throws IOException, GitAPIException {
        final String remoteName = modConfig.get(PUSH_REMOTE_NAME);
        debug(logger, "doing naive push");
        git.push().setRemote(remoteName).setRefSpecs(new RefSpec(branchNameToPush + ":" + branchNameToPush)).call();
        debug(logger, () -> "checking out " + branchNameToPush);
    }

    private static void configureRemoteUpstream(final ModConfig modConfig, final Path worldSaveDir) throws IOException, GitAPIException, URISyntaxException {
        final String remoteName = requireNonNull(modConfig.get(PUSH_REMOTE_NAME));
        final String remoteUrl = modConfig.get(REMOTE_UPSTREAM_URI);
        if (remoteUrl == null || remoteUrl.isEmpty()) {
            throw new IOException("Remote push is enabled but " + REMOTE_UPSTREAM_URI + " is not configured.");
        }
        final URIish remoteUri = new URIish().setRawPath(remoteUrl);
        setRemote(remoteName, remoteUri, worldSaveDir);
    }

    private static void configureFileUpstream(final ModConfig config, final Path worldSaveDir, Loginator logger) throws IOException, GitAPIException {
        // ensure a git repository exists to push at the configured path
        final String uuid = getWorldUuid(worldSaveDir);
        final Path fupHome = Path.of(config.get(FILE_UPSTREAM_PATH)).resolve(uuid);
        final Path fupGitDir = fupHome.resolve("git");
        mkdirs(fupGitDir);
        if (worldSaveDir.resolve(WORLD_INFO_PATH).toFile().exists()) {
            Files.copy(worldSaveDir.resolve(WORLD_INFO_PATH), fupHome.resolve("world-info.properties"), REPLACE_EXISTING);
        }
        Git git = Git.init().setBare(config.getBoolean(FILE_UPSTREAM_BARE)).setDirectory(fupGitDir.toFile()).call();
        final String rawConfig = config.get(FILE_UPSTREAM_GIT_CONFIG).replace(';', '\n');
        debug(logger, "updating upstream git config");
        GitUtils.mergeGitConfig(git, rawConfig, logger);
        // configure the world save git dir the push to it
        final String remoteName = config.get(PUSH_REMOTE_NAME);
        final URIish fileUri = new URIish().setScheme("file").setPath(fupGitDir.toString());
        setRemote(remoteName, fileUri, worldSaveDir);
    }

    private static void setRemote(String remoteName, URIish uri, final Path worldSaveDir) throws IOException, GitAPIException {
        final Git worldGit = Git.open(worldSaveDir.toFile());
        worldGit.remoteRemove().setRemoteName(remoteName).call();
        worldGit.remoteAdd().setName(remoteName).setUri(uri).call();
    }

    private static boolean doUuidCheck(ModConfig config, Git git, String remoteName, Path worldSaveDir, Loginator logger) throws GitAPIException, IOException {
        if (!config.getBoolean(PUSH_UUID_CHECK_ENABLED)) {
            logger.warn("World uuid check has been disabled.  You're in danger of mixing backups.");
            return true;
        }
        final Set<String> remoteWorldUuids = getWorldUuidsOnRemote(git, remoteName, logger);
        final String localUuid = getWorldUuid(worldSaveDir);
        if (remoteWorldUuids.size() > 2) {
            warn(logger, "Remote has more than one world-uuid.  This unusual. " + remoteWorldUuids);
        }
        if (remoteWorldUuids.isEmpty()) {
            debug(logger, "Remote does not have any previously-backed up worlds.");
        } else {
            if (!remoteWorldUuids.contains(localUuid)) {
                final URIish remoteUri = GitUtils.getRemoteUri(git, remoteName, logger);
                error(logger, "Remote at " + remoteUri + " is a backup target for a different world.");
                error(logger, "Please configure a new remote for backing up this world.");
                error(logger, "local =" + localUuid);
                error(logger, "remote =" + remoteWorldUuids);
                return false;
            }
        }
        debug(logger, "world-uuid check passed.");
        return true;
    }

    public static Set<String> getWorldUuidsOnRemote(Git git, String remoteName, Loginator logger) throws GitAPIException {
        final Set<String> remoteBranchNames = GitUtils.getRemoteBranchNames(git, remoteName, logger);
        final Set<String> out = new HashSet<>();
        for (String branchName : remoteBranchNames) {
            final String worldUuid = BranchNameUtils.extractWorldUuid(branchName, logger);
            if (worldUuid != null) out.add(worldUuid);
        }
        return out;
    }
}
