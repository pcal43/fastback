package net.pcal.fastback;

import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.BranchNameUtils.createSnapshotBranchName;
import static net.pcal.fastback.LogUtils.error;
import static net.pcal.fastback.LogUtils.info;
import static net.pcal.fastback.WorldUtils.getWorldUuid;
import static net.pcal.fastback.Task.TaskState.FAILED;

@SuppressWarnings("FieldCanBeLocal")
public class BackupTask extends Task {

    private final ModConfig modConfig;
    private final MinecraftServer server;
    private final Logger logger;

    BackupTask(final ModConfig modConfig, final MinecraftServer server, final Logger logger) {
        this.server = requireNonNull(server);
        this.modConfig = requireNonNull(modConfig);
        this.logger = requireNonNull(logger);
    }

    public void run() {
        final Path worldSaveDir = MinecraftUtils.getWorldSaveDir(this.server);
        final String worldUuid;
        try {
            worldUuid = getWorldUuid(worldSaveDir);
        } catch (IOException e) {
            error(logger, "Local backup failed.  Could not determine world-uuid.", e);
            return;
        }
        final String newBranchName = createSnapshotBranchName(worldUuid, logger);
        info(logger, "Creating "+newBranchName);
        try {
            CommitUtils.doCommit(modConfig, worldSaveDir, newBranchName, logger);
        } catch (GitAPIException | IOException e) {
            error(logger, "Local backup failed.  Unable to commit changes.", e);
            super.setState(FAILED);
            return;
        }
        try {
            PushUtils.pushIfNecessary(newBranchName, modConfig, worldSaveDir, logger);
        } catch (IOException | GitAPIException e) {
            error(logger, "Local backup succeeded but remote backup failed.", e);
        }
    }
}
