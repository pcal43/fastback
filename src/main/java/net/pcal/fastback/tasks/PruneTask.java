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

import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.retention.RetentionPolicy;
import net.pcal.fastback.retention.RetentionPolicyCodec;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.Collection;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.tasks.ListSnapshotsTask.listSnapshotsForWorldSorted;

/**
 * Runs git garbage collection.  Aggressively deletes reflogs, tracking branches and stray temporary branches
 * in an attempt to free up objects and reclaim disk space.
 *
 * @author pcal
 * @since 0.0.12
 */
public class PruneTask implements Runnable {

    private final ModContext ctx;
    private final Logger log;
    private final Git git;

    public PruneTask(final Git git,
                     final ModContext ctx,
                     final Logger log) {
        this.git = requireNonNull(git);
        this.ctx = requireNonNull(ctx);
        this.log = requireNonNull(log);
    }

    public void run() {
        final WorldConfig wc;
        try {
            wc = WorldConfig.load(git);
        } catch (IOException e) {
            log.internalError("Failed to load config", e);
            return;
        }
        final String policyConfig = wc.retentionPolicy();
        if (policyConfig == null) {
            log.notifyError(localized("fastback.notify.prune-no-default"));
            return;
        }
        final RetentionPolicy policy = RetentionPolicyCodec.INSTANCE.decodePolicy
                (ctx, ctx.getAvailableRetentionPolicyTypes(), policyConfig);
        if (policy == null) {
            log.notifyError(localized("fastback.notify.retention-policy-not-set"));
            return;
        }
        final Collection<SnapshotId> toPrune =
                policy.getSnapshotsToPrune(listSnapshotsForWorldSorted(git, ctx.getLogger()));
        int pruned = 0;
        for (final SnapshotId sid : toPrune) {
            log.notify(localized("fastback.notify.prune-pruning", sid.getName()));
            try {
                git.branchDelete().setForce(true).setBranchNames(new String[]{sid.getBranchName()}).call();
                pruned++;
            } catch (final GitAPIException e) {
                log.internalError("failed to prune branch for " + sid, e);
            }
        }
        log.notify(localized("fastback.notify.prune-done", pruned));
        if (pruned > 0) {
            log.notify(localized("fastback.notify.prune-suggest-gc"));
        }
    }
}
