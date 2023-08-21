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
import net.pcal.fastback.config.GitConfigKey;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.retention.RetentionPolicy;
import net.pcal.fastback.retention.RetentionPolicyCodec;
import net.pcal.fastback.retention.RetentionPolicyType;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.config.GitConfigKey.LOCAL_RETENTION_POLICY;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.logging.Message.localizedError;
import static net.pcal.fastback.repo.SnapshotId.sortWorldSnapshots;

/**
 * Delete local snapshot branches that should not be kept per the retention policy.
 *
 * @author pcal
 * @since 0.3.0
 */
//TODO write PruneUtils instead
class JGitLocalPruneTask implements Callable<Collection<SnapshotId>> {

    private final Logger log;
    private final RepoImpl repo;

    JGitLocalPruneTask(final RepoImpl repo,
                       final Logger log) {
        this.repo = requireNonNull(repo);
        this.log = requireNonNull(log);
    }

    @Override
    public Collection<SnapshotId> call() throws IOException {
        return doPrune(repo, log,
                LOCAL_RETENTION_POLICY,
                repo::listSnapshots,
                sid -> {
                    log.info("Pruning local snapshot " + sid.getName());
                    try {
                        repo.getJGit().branchDelete().setForce(true).setBranchNames(new String[]{sid.getBranchName()}).call();
                    } catch (GitAPIException e) {
                        throw new IOException(e);
                    }
                },
                "fastback.chat.retention-policy-not-set"
        );
    }

    static Collection<SnapshotId> doPrune(Repo repo,
                                          Logger log,
                                          GitConfigKey policyConfigKey,
                                          JGitSupplier<ListMultimap<String, SnapshotId>> listSnapshotsFn,
                                          JGitConsumer<SnapshotId> deleteSnapshotsFn,
                                          String notSetKey) throws IOException {
        final GitConfig conf = repo.getConfig();
        final String policyConfig = conf.getString(policyConfigKey);
        final RetentionPolicy policy = RetentionPolicyCodec.INSTANCE.decodePolicy(RetentionPolicyType.getAvailable(), policyConfig);
        if (policy == null) {
            log.chat(localizedError(notSetKey));
            return null;
        }
        final Collection<SnapshotId> toPrune = policy.getSnapshotsToPrune(
                sortWorldSnapshots(listSnapshotsFn.get(), repo.getWorldUuid()));
        log.hud(localized("fastback.hud.prune-started"));
        for (final SnapshotId sid : toPrune) {
            deleteSnapshotsFn.accept(sid);
        }
        return toPrune;
    }
}
