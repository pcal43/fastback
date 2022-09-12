package net.pcal.fastback.tasks;

import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.BranchNameUtils.filterOnWorldUuid;
import static net.pcal.fastback.tasks.Task.TaskState.COMPLETED;
import static net.pcal.fastback.tasks.Task.TaskState.FAILED;
import static net.pcal.fastback.tasks.Task.TaskState.STARTED;

public class ListSnapshotsTask extends Task {

    private final Path worldSaveDir;
    private final Consumer<String> out;
    private final Function<String, String> branchFilter;
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
        final Function<String, String> branchFilter = branchName -> filterOnWorldUuid(branchName, worldUuid, logger);
        return new ListSnapshotsTask(worldSaveDir, branchFilter, sink, logger);
    }

    private ListSnapshotsTask(Path worldSaveDir, Function<String, String> branchFilter, Consumer<String> sink, Logger logger) {
        this.worldSaveDir = requireNonNull(worldSaveDir);
        this.out = requireNonNull(sink);
        this.branchFilter = requireNonNull(branchFilter);
        this.logger = requireNonNull(logger);
    }

    private static final String BRANCH_NAME_PREFIX = "refs/heads";

    @Override
    public void run() {
        super.setState(STARTED);
        try (final Git git = Git.open(worldSaveDir.toFile())) {
            for (Ref branch : git.branchList().call()) {
                String branchName = branch.getName();
                if (!branchName.startsWith(BRANCH_NAME_PREFIX)) {
                    this.logger.warn("Unexpected ref name " + branchName);
                } else {
                    branchName = branchName.substring(BRANCH_NAME_PREFIX.length() + 1);
                }
                branchName = this.branchFilter.apply(branchName);
                if (branchName != null) this.out.accept(branchName);
            }
        } catch (GitAPIException | IOException e) {
            super.setState(FAILED);
            throw new RuntimeException(e);
        }
        super.setState(COMPLETED);
    }
}
