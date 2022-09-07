package net.pcal.fastback.tasks;

import net.pcal.fastback.CommitUtils;
import net.pcal.fastback.Loggr;
import net.pcal.fastback.ModConfig;
import net.pcal.fastback.WorldConfig;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.BranchNameUtils.createNewSnapshotBranchName;
import static net.pcal.fastback.WorldUtils.getWorldUuid;
import static net.pcal.fastback.tasks.Task.TaskState.FAILED;

@SuppressWarnings("FieldCanBeLocal")
public class BackupTask extends Task {

    private final WorldConfig worldConfig;

    @Deprecated
    private final ModConfig modConfig;
    private final Path worldSaveDir;
    private final Loggr logger;

    public BackupTask(final ModConfig modConfig, final Path worldSaveDir, TaskListener listener, final Loggr logger) {
        this.worldConfig = requireNonNull(modConfig.getWorldConfig());
        this.worldSaveDir = requireNonNull(worldSaveDir);
        this.modConfig = requireNonNull(modConfig);
        this.listener = requireNonNull(listener);
        this.logger = requireNonNull(logger);
    }

    public void run() {
        final String worldUuid;
        try {
            worldUuid = getWorldUuid(worldSaveDir);
        } catch (IOException e) {
            logger.error("Local backup failed.  Could not determine world-uuid.", e);
            return;
        }
        final String newBranchName = createNewSnapshotBranchName(worldUuid);
        logger.info("Creating " + newBranchName);
        try {
            CommitUtils.doCommit(modConfig, worldSaveDir, newBranchName, logger);
        } catch (GitAPIException | IOException e) {
            logger.error("Local backup failed.  Unable to commit changes.", e);
            super.setState(FAILED);
            return;
        }
        if (worldConfig.isRemoteBackupEnabled()) {
            final PushTask push = new PushTask(worldSaveDir, newBranchName, tl, logger);
            push.run();
            if (push.isFailed()) {
                logger.error("Local backup succeeded but remote backup failed.");
            }
        } else {
            logger.info("Remote backup disabled in config.");
        }
    }
}
