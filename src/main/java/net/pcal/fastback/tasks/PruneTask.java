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
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.tasks.ListSnapshotsTask.listSnapshots;
import static net.pcal.fastback.tasks.ListSnapshotsTask.sortWorldSnapshots;

/**
 * Runs git garbage collection.  Aggressively deletes reflogs, tracking branches and stray temporary branches
 * in an attempt to free up objects and reclaim disk space.
 *
 * @author pcal
 * @since 0.0.12
 */
public class PruneTask implements Callable<Collection<SnapshotId>> {

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

    @Override
    public Collection<SnapshotId> call() throws IOException, GitAPIException {
        final WorldConfig wc = WorldConfig.load(git);
        final String policyConfig = wc.retentionPolicy();
        if (policyConfig == null) {
            log.chatError(localized("fastback.chat.prune-no-default"));
            return null;
        }
        final RetentionPolicy policy = RetentionPolicyCodec.INSTANCE.decodePolicy
                (ctx, ctx.getRetentionPolicyTypes(), policyConfig);
        if (policy == null) {
            log.chatError(localized("fastback.chat.retention-policy-not-set"));
            return null;
        }
        final Collection<SnapshotId> toPrune = policy.getSnapshotsToPrune(
                sortWorldSnapshots(listSnapshots(git, ctx.getLogger()), wc.worldUuid()));
        log.hud(localized("fastback.hud.prune-started"));
        for (final SnapshotId sid : toPrune) {
            log.info("Pruning " + sid.getName());
            git.branchDelete().setForce(true).setBranchNames(new String[]{sid.getBranchName()}).call();
        }
        return toPrune;
    }
}
