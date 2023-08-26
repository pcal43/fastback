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
import net.pcal.fastback.config.FastbackConfigKey;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.retention.RetentionPolicy;
import net.pcal.fastback.retention.RetentionPolicyCodec;
import net.pcal.fastback.retention.RetentionPolicyType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RefSpec;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static net.pcal.fastback.config.FastbackConfigKey.LOCAL_RETENTION_POLICY;
import static net.pcal.fastback.config.FastbackConfigKey.REMOTE_NAME;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.logging.UserMessage.styledLocalized;

/**
 * Utils for pruning and deleting snapshot branches.
 *
 * @author pcal
 * @since 0.13.0
 */
class PruneUtils {

    static void deleteRemoteBranch(final RepoImpl repo, String remoteBranchName) throws IOException {
        RefSpec refSpec = new RefSpec()
                .setSource(null)
                .setDestination("refs/heads/" + remoteBranchName);
        try {
            repo.getJGit().push().setRefSpecs(refSpec).setRemote(repo.getConfig().getString(REMOTE_NAME)).call();
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }

    static void deleteLocalBranches(final RepoImpl repo, List<String> branchNames) throws IOException {
        try {
            repo.getJGit().branchDelete().setForce(true).setBranchNames(branchNames.toArray(new String[0])).call();
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }

    static Collection<SnapshotId> doLocalPrune(final RepoImpl repo, final UserLogger log) throws IOException {
        return doPrune(repo, log,
                LOCAL_RETENTION_POLICY,
                repo::listSnapshots,
                sid -> {
                    syslog().info("Pruning local snapshot " + sid.getBranchName());
                    deleteLocalBranches(repo, List.of(sid.getBranchName()));
                },
                "fastback.chat.retention-policy-not-set"
        );
    }

    static Collection<SnapshotId> doRemotePrune(RepoImpl repo, UserLogger ulog) throws IOException {
        return doPrune(repo, ulog,
                FastbackConfigKey.REMOTE_RETENTION_POLICY,
                repo::listRemoteSnapshots,
                sid -> {
                    syslog().info("Pruning remote snapshot " + sid.getBranchName());
                    repo.deleteRemoteBranch(sid.getBranchName());
                },
                "fastback.chat.remote-retention-policy-not-set"
        );
    }

    private static Collection<SnapshotId> doPrune(Repo repo,
                                                  UserLogger log,
                                                  FastbackConfigKey policyConfigKey,
                                                  JGitSupplier<ListMultimap<WorldId, SnapshotId>> listSnapshotsFn,
                                                  JGitConsumer<SnapshotId> deleteSnapshotsFn,
                                                  String notSetKey) throws IOException {
        final GitConfig conf = repo.getConfig();
        RetentionPolicy policy = null;
        final String policyConfig = conf.getString(policyConfigKey);
        if (policyConfig != null) {
            policy = RetentionPolicyCodec.INSTANCE.decodePolicy(RetentionPolicyType.getAvailable(), policyConfig);
        }
        if (policy == null) {
            log.message(styledLocalized(notSetKey, ERROR));
            return null;
        }
        final Collection<SnapshotId> toPrune = policy.getSnapshotsToPrune(
                Repo.sortWorldSnapshots(listSnapshotsFn.get(), repo.getWorldId()));
        log.update(UserMessage.localized("fastback.hud.prune-started"));
        for (final SnapshotId sid : toPrune) {
            deleteSnapshotsFn.accept(sid);
        }
        return toPrune;
    }
}
