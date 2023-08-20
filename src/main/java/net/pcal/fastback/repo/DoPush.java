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
import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.merge.ContentMergeStrategy;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.config.GitConfigKey.IS_NATIVE_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.IS_REMOTE_TEMP_BRANCH_CLEANUP_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.IS_SMART_PUSH_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.IS_TEMP_BRANCH_CLEANUP_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.IS_TRACKING_BRANCH_CLEANUP_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.IS_UUID_CHECK_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.REMOTE_NAME;
import static net.pcal.fastback.config.GitConfigKey.REMOTE_PUSH_URL;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.logging.Message.localizedError;
import static net.pcal.fastback.repo.DoExec.doExec;

class DoPush {

    static boolean isTempBranch(String branchName) {
        return branchName.startsWith("temp/");
    }

    static void doPush(SnapshotId sid, RepoImpl repo, Logger log) throws IOException {
        try {
            log.hud(localized("fastback.hud.remote-uploading", 0));
            final GitConfig conf = repo.getConfig();
            final String pushUrl = conf.getString(REMOTE_PUSH_URL);
            if (pushUrl == null) {
                final String msg = "Skipping remote backup because no remote url has been configured.";
                log.warn(msg);
                return;
            }
            final Git jgit = repo.getJGit();
            final Collection<Ref> remoteBranchRefs = jgit.lsRemote().setHeads(true).setTags(false).
                    setRemote(conf.getString(REMOTE_NAME)).call();
            final ListMultimap<String, SnapshotId> snapshotsPerWorld =
                    SnapshotId.getSnapshotsPerWorld(remoteBranchRefs, log);
            if (conf.getBoolean(IS_UUID_CHECK_ENABLED)) {
                final boolean uuidCheckResult;
                try {
                    uuidCheckResult = jgit_doUuidCheck(repo, snapshotsPerWorld.keySet(), conf, log);
                } catch (final IOException e) {
                    log.internalError("Skipping remote backup due to failed uuid check", e);
                    return;
                }
                if (!uuidCheckResult) {
                    log.warn("Skipping remote backup due to world mismatch.");
                    return;
                }
            }
            log.info("Pushing to " + pushUrl);
            if (conf.getBoolean(IS_NATIVE_ENABLED)) {
                native_doPush(repo, sid.getBranchName(), log);
            } else if (conf.getBoolean(IS_SMART_PUSH_ENABLED)) {
                final String uuid = repo.getWorldUuid();
                jgit_doSmartPush(repo, snapshotsPerWorld.get(uuid), sid.getBranchName(), conf, log);
            } else {
                jgit_doNaivePush(jgit, sid.getBranchName(), conf, log);
            }
            log.info("Remote backup complete.");
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }

    private static void native_doPush(final Repo repo, final String branchNameToPush, final Logger log) throws IOException {
        log.debug("Starting native_push");
        final File worktree = repo.getWorkTree();
        final GitConfig conf = repo.getConfig();
        String remoteName = conf.getString(REMOTE_NAME);
        final String[] push = { "git", "-C", worktree.getAbsolutePath(), "push", "--set-upstream", remoteName, branchNameToPush };
        doExec(push, log);
        log.debug("Done native_push");
    }

    private static void jgit_doSmartPush(final RepoImpl repo, List<SnapshotId> remoteSnapshots, final String branchNameToPush, final GitConfig conf, final Logger logger) throws IOException {
        try {
            final Git jgit = repo.getJGit();
            final String remoteName = conf.getString(REMOTE_NAME);
            final String worldUuid = repo.getWorldUuid();
            final SnapshotId latestCommonSnapshot;
            if (remoteSnapshots.isEmpty()) {
                logger.warn("** This appears to be the first time this world has been pushed.");
                logger.warn("** If the world is large, this may take some time.");
                jgit_doNaivePush(jgit, branchNameToPush, conf, logger);
                return;
            } else {
                final Collection<Ref> localBranchRefs = jgit.branchList().call();
                final ListMultimap<String, SnapshotId> localSnapshotsPerWorld =
                        SnapshotId.getSnapshotsPerWorld(localBranchRefs, logger);
                final List<SnapshotId> localSnapshots = localSnapshotsPerWorld.get(worldUuid);
                remoteSnapshots.retainAll(localSnapshots);
                if (remoteSnapshots.isEmpty()) {
                    logger.warn("No common snapshots found between local and remote.");
                    logger.warn("Doing a full push.  This may take some time.");
                    jgit_doNaivePush(jgit, branchNameToPush, conf, logger);
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
            jgit.checkout().setCreateBranch(true).setName(tempBranchName).call();
            final ObjectId branchId = jgit.getRepository().resolve(latestCommonSnapshot.getBranchName());
            logger.debug("Merging " + latestCommonSnapshot.getBranchName());
            jgit.merge().setContentMergeStrategy(ContentMergeStrategy.OURS).
                    include(branchId).setMessage("Merge " + branchId + " into " + tempBranchName).call();
            logger.debug("Checking out " + branchNameToPush);
            jgit.checkout().setName(branchNameToPush).call();
            logger.debug("Pushing temp branch " + tempBranchName);
            final ProgressMonitor pm = new JGitIncrementalProgressMonitor(new JGitPushProgressMonitor(logger), 100);
            final Iterable<PushResult> pushResult = jgit.push().setProgressMonitor(pm).setRemote(remoteName).
                    setRefSpecs(new RefSpec(tempBranchName + ":" + tempBranchName),
                            new RefSpec(branchNameToPush + ":" + branchNameToPush)).call();
            logger.info("Cleaning up branches...");
            if (conf.getBoolean(IS_TRACKING_BRANCH_CLEANUP_ENABLED)) {
                for (final PushResult pr : pushResult) {
                    for (final TrackingRefUpdate f : pr.getTrackingRefUpdates()) {
                        final String PREFIX = "refs/remotes/";
                        if (f.getLocalName().startsWith(PREFIX)) {
                            final String trackingBranchName = f.getLocalName().substring(PREFIX.length());
                            logger.info("Cleaning up tracking branch " + trackingBranchName);
                            jgit.branchDelete().setForce(true).setBranchNames(trackingBranchName).call();
                        } else {
                            logger.warn("Ignoring unrecognized TrackingRefUpdate " + f.getLocalName());
                        }
                    }
                }
            }
            if (conf.getBoolean(IS_TEMP_BRANCH_CLEANUP_ENABLED)) {
                logger.info("Deleting local temp branch " + tempBranchName);
                jgit.branchDelete().setForce(true).setBranchNames(tempBranchName).call();
            }
            if (conf.getBoolean(IS_REMOTE_TEMP_BRANCH_CLEANUP_ENABLED)) {
                final String remoteTempBranch = "refs/heads/" + tempBranchName;
                logger.info("Deleting remote temp branch " + remoteTempBranch);
                final RefSpec deleteRemoteBranchSpec = new RefSpec().setSource(null).setDestination(remoteTempBranch);
                jgit.push().setProgressMonitor(pm).setRemote(remoteName).setRefSpecs(deleteRemoteBranchSpec).call();
            }
            logger.info("Push complete");
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }

    private static void jgit_doNaivePush(final Git jgit, final String branchNameToPush, final GitConfig conf, final Logger logger) throws IOException {
        final ProgressMonitor pm = new JGitIncrementalProgressMonitor(new JGitPushProgressMonitor(logger), 100);
        final String remoteName = conf.getString(REMOTE_NAME);
        logger.info("Doing naive push of " + branchNameToPush);
        try {
            jgit.push().setProgressMonitor(pm).setRemote(remoteName).
                    setRefSpecs(new RefSpec(branchNameToPush + ":" + branchNameToPush)).call();
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }

    private static boolean jgit_doUuidCheck(RepoImpl repo, Set<String> remoteWorldUuids, GitConfig config, Logger logger) throws IOException {
        final String localUuid = repo.getWorldUuid();
        if (remoteWorldUuids.size() > 2) {
            logger.warn("Remote has more than one world-uuid.  This is unusual. " + remoteWorldUuids);
        }
        if (remoteWorldUuids.isEmpty()) {
            logger.debug("Remote does not have any previously-backed up worlds.");
        } else {
            if (!remoteWorldUuids.contains(localUuid)) {
                final URIish remoteUri = jgit_getRemoteUri(repo.getJGit(), config.getString(REMOTE_NAME), logger);
                logger.chat(localizedError("fastback.chat.push-uuid-mismatch", remoteUri));
                logger.info("local: " + localUuid + ", remote: " + remoteWorldUuids);
                return false;
            }
        }
        logger.debug("world-uuid check passed.");
        return true;
    }

    private static String getTempBranchName(String uniqueName) {
        return "temp/" + uniqueName;
    }

    private static URIish jgit_getRemoteUri(Git jgit, String remoteName, Logger logger) throws IOException {
        requireNonNull(jgit);
        requireNonNull(remoteName);
        final List<RemoteConfig> remotes;
        try {
            remotes = jgit.remoteList().call();
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
        for (final RemoteConfig remote : remotes) {
            logger.debug("getRemoteUri " + remote);
            if (remote.getName().equals(remoteName)) {
                return remote.getPushURIs() != null && !remote.getURIs().isEmpty() ? remote.getURIs().get(0) : null;
            }
        }
        return null;
    }

    private static class JGitPushProgressMonitor extends JGitPercentageProgressMonitor {

        private final Logger logger;

        public JGitPushProgressMonitor(Logger logger) {
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
                this.logger.hud(localized("fastback.hud.remote-preparing", percentage / 2));
            } else if (task.contains("Writing objects")) {
                this.logger.hud(localized("fastback.hud.remote-uploading", 50 + (percentage / 2)));
            }
        }

        @Override
        public void progressDone(String task) {
            logger.info("Done " + task);
        }

        @Override
        public void showDuration(boolean enabled) {
        }
    }
}
