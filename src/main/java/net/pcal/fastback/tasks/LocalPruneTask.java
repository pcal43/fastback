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
import net.pcal.fastback.config.RepoConfig;
import net.pcal.fastback.config.RepoConfigKey;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.retention.RetentionPolicy;
import net.pcal.fastback.retention.RetentionPolicyCodec;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.config.RepoConfigKey.LOCAL_RETENTION_POLICY;
import static net.pcal.fastback.config.RepoConfigUtils.getWorldUuid;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.tasks.ListSnapshotsTask.listSnapshots;
import static net.pcal.fastback.utils.SnapshotId.sortWorldSnapshots;

/**
 * Delete local snapshot branches that should not be kept per the retention policy.
 *
 * @author pcal
 * @since 0.3.0
 */
public class LocalPruneTask implements Callable<Collection<SnapshotId>> {

    private final ModContext ctx;
    private final Logger log;
    private final Git jgit;

    public LocalPruneTask(final Git git,
                          final ModContext ctx,
                          final Logger log) {
        this.jgit = requireNonNull(git);
        this.ctx = requireNonNull(ctx);
        this.log = requireNonNull(log);
    }

    @Override
    public Collection<SnapshotId> call() throws IOException, GitAPIException {
        return doPrune(jgit, ctx, log,
                LOCAL_RETENTION_POLICY,
                () -> listSnapshots(jgit, ctx.getLogger()),
                sid -> {
                    log.info("Pruning local snapshot " + sid.getName());
                    jgit.branchDelete().setForce(true).setBranchNames(new String[]{sid.getBranchName()}).call();
                },
                "fastback.chat.retention-policy-not-set"
        );
    }

    static Collection<SnapshotId> doPrune(Git jgit,
                                          ModContext ctx,
                                          Logger log,
                                          RepoConfigKey policyConfigKey,
                                          JGitSupplier<ListMultimap<String, SnapshotId>> listSnapshotsFn,
                                          JGitConsumer<SnapshotId> deleteSnapshotsFn,
                                          String notSetKey) throws IOException, GitAPIException {
        final RepoConfig conf = RepoConfig.load(jgit);
        final String policyConfig = conf.getString(policyConfigKey);
        final RetentionPolicy policy = RetentionPolicyCodec.INSTANCE.decodePolicy
                (ctx, ctx.getRetentionPolicyTypes(), policyConfig);
        if (policy == null) {
            log.chatError(localized(notSetKey));
            return null;
        }
        final Collection<SnapshotId> toPrune = policy.getSnapshotsToPrune(
                sortWorldSnapshots(listSnapshotsFn.get(), getWorldUuid(jgit)));
        log.hud(localized("fastback.hud.prune-started"));
        for (final SnapshotId sid : toPrune) {
            deleteSnapshotsFn.accept(sid);
        }
        return toPrune;
    }
}
