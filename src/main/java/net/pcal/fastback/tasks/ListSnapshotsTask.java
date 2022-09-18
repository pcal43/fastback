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
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.tasks.Task.TaskState.COMPLETED;
import static net.pcal.fastback.tasks.Task.TaskState.FAILED;
import static net.pcal.fastback.tasks.Task.TaskState.STARTED;
import static net.pcal.fastback.utils.SnapshotId.getSnapshotsPerWorld;

@SuppressWarnings({"Convert2MethodRef", "FunctionalExpressionCanBeFolded"})
public class ListSnapshotsTask extends Task {

    private final Path worldSaveDir;
    private final Consumer<SnapshotId> sink;
    private final String worldUuid;
    private final Logger logger;

    public static List<SnapshotId> listSnapshotsForWorldSorted(Path worldSaveDir, Logger log) {
        final List<SnapshotId> out = new ArrayList<>();
        listSnapshotsForWorld(worldSaveDir, s -> out.add(s), log).run();
        Collections.sort(out);
        return out;
    }

    public static Runnable listSnapshotsForWorld(Path worldSaveDir, Consumer<SnapshotId> sink, Logger logger) {
        final String worldUuid;
        try {
            worldUuid = WorldConfig.getWorldUuid(worldSaveDir);
        } catch (IOException e) {
            logger.internalError("Could not load world Uuid", e);
            return null;//FIXME
        }
        return listSnapshotsForWorld(worldSaveDir, worldUuid, sink, logger);
    }

    public static Runnable listSnapshotsForWorld(Path worldSaveDir, String worldUuid, Consumer<SnapshotId> sink, Logger logger) {
        return new ListSnapshotsTask(worldSaveDir, worldUuid, sink, logger);
    }

    private ListSnapshotsTask(Path worldSaveDir, String worlduuid, Consumer<SnapshotId> sink, Logger logger) {
        this.worldUuid = requireNonNull(worlduuid);
        this.worldSaveDir = requireNonNull(worldSaveDir);
        this.sink = requireNonNull(sink);
        this.logger = requireNonNull(logger);
    }

    @Override
    public void run() {
        super.setState(STARTED);
        try (final Git git = Git.open(worldSaveDir.toFile())) {
            Collection<Ref> localBranchRefs = git.branchList().call();
            ListMultimap<String, SnapshotId> snapshotsPerWorld = getSnapshotsPerWorld(localBranchRefs, logger);
            List<SnapshotId> snapshots = snapshotsPerWorld.get(worldUuid);
            snapshots.forEach(sid -> this.sink.accept(sid));
        } catch (GitAPIException | IOException e) {
            super.setState(FAILED);
            throw new RuntimeException(e);
        }
        super.setState(COMPLETED);
    }
}
