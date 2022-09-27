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
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.tasks.Task.TaskState.COMPLETED;
import static net.pcal.fastback.tasks.Task.TaskState.STARTED;
import static net.pcal.fastback.utils.SnapshotId.getSnapshotsPerWorld;

@SuppressWarnings({"Convert2MethodRef", "FunctionalExpressionCanBeFolded"})
public class ListSnapshotsTask extends Task {

    private final Git git;
    private final Consumer<SnapshotId> sink;
    private final Logger logger;

    public static List<SnapshotId> listSnapshotsForWorldSorted(final Git git, final Logger log) {
        final List<SnapshotId> out = new ArrayList<>();
        new ListSnapshotsTask(git, log, s -> out.add(s)).run();
        Collections.sort(out);
        return out;
    }

    public ListSnapshotsTask(Git git, Logger logger, Consumer<SnapshotId> sink) {
        this.git = requireNonNull(git);
        this.sink = requireNonNull(sink);
        this.logger = requireNonNull(logger);
    }

    @Override
    public void run() {
        super.setState(STARTED);
        try {
            final WorldConfig wc = WorldConfig.load(git);
            final Collection<Ref> localBranchRefs = git.branchList().call();
            final ListMultimap<String, SnapshotId> snapshotsPerWorld = getSnapshotsPerWorld(localBranchRefs, logger);
            List<SnapshotId> snapshots = snapshotsPerWorld.get(wc.worldUuid());
            snapshots.forEach(sid -> this.sink.accept(sid));
            super.setState(COMPLETED);
        } catch (final Exception e) {
            this.logger.internalError("failed to list snapshots", e);
        }
    }
}
