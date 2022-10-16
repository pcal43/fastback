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
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.tasks.ListSnapshotsTask.listRemoteSnapshots;
import static net.pcal.fastback.tasks.LocalPruneTask.doPrune;
import static net.pcal.fastback.utils.GitUtils.deleteRemoteBranch;

/**
 * Delete remote snapshot branches that should not be kept per the retention policy.
 *
 * @author pcal
 * @since 0.7.0
 */
public class RemotePruneTask implements Callable<Collection<SnapshotId>> {

    private final ModContext ctx;
    private final Logger log;
    private final Git git;

    public RemotePruneTask(final Git git,
                           final ModContext ctx,
                           final Logger log) {
        this.git = requireNonNull(git);
        this.ctx = requireNonNull(ctx);
        this.log = requireNonNull(log);
    }

    @Override
    public Collection<SnapshotId> call() throws IOException, GitAPIException {
        final WorldConfig wc = WorldConfig.load(git);
        return doPrune(wc, ctx, log,
                wc::remoteRetentionPolicy,
                () -> listRemoteSnapshots(git, wc, ctx.getLogger()),
                sid -> {
                    log.info("Pruning remote snapshot " + sid.getName());
                    deleteRemoteBranch(git, wc.getRemoteName(), sid.getBranchName());
                },
                "fastback.chat.remote-retention-policy-not-set"
        );
    }
}
