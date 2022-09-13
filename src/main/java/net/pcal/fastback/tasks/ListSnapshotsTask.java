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
import static net.pcal.fastback.tasks.Task.TaskState.*;
import static net.pcal.fastback.utils.SnapshotId.getSnapshotsPerWorld;

public class ListSnapshotsTask extends Task {

    private final Path worldSaveDir;
    private final Consumer<String> out;
    private final String worldUuid;
    private final Logger logger;


    public static List<String> listSnapshotsForWorldSorted(Path worldSaveDir, Logger log) {
        final List<String> out = new ArrayList<>();
        listSnapshotsForWorld(worldSaveDir, s -> out.add(s), log).run();
        Collections.sort(out);
        return out;
    }

    public static Runnable listSnapshotsForWorld(Path worldSaveDir, Consumer<String> sink, Logger logger) {
        final String worldUuid;
        try {
            worldUuid = WorldConfig.getWorldUuid(worldSaveDir);
        } catch (IOException e) {
            sink.accept("Internal error encountered.  See logs for details.");
            logger.internalError("Could not load world Uuid", e);
            return null;//FIXME
        }
        return listSnapshotsForWorld(worldSaveDir, worldUuid, sink, logger);
    }

    public static Runnable listSnapshotsForWorld(Path worldSaveDir, String worldUuid, Consumer<String> sink, Logger logger) {
        return new ListSnapshotsTask(worldSaveDir, worldUuid, sink, logger);
    }

    private ListSnapshotsTask(Path worldSaveDir, String worlduuid, Consumer<String> sink, Logger logger) {
        this.worldUuid = requireNonNull(worlduuid);
        this.worldSaveDir = requireNonNull(worldSaveDir);
        this.out = requireNonNull(sink);
        this.logger = requireNonNull(logger);
    }

    @Override
    public void run() {
        super.setState(STARTED);
        try (final Git git = Git.open(worldSaveDir.toFile())) {
            Collection<Ref> localBranchRefs = git.branchList().call();
            ListMultimap<String, SnapshotId> snapshotsPerWorld = getSnapshotsPerWorld(localBranchRefs, logger);
            List<SnapshotId> snapshots = snapshotsPerWorld.get(worldUuid);
            snapshots.forEach(sid -> this.out.accept(sid.getBranchName()));
        } catch (GitAPIException | IOException e) {
            super.setState(FAILED);
            throw new RuntimeException(e);
        }
        super.setState(COMPLETED);
    }
}
