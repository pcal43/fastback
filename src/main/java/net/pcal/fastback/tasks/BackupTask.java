package net.pcal.fastback.tasks;

import net.pcal.fastback.CommitUtils;
import net.pcal.fastback.Loginator;
import net.pcal.fastback.ModConfig;
import net.pcal.fastback.PushUtils;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.BranchNameUtils.createSnapshotBranchName;
import static net.pcal.fastback.LogUtils.error;
import static net.pcal.fastback.LogUtils.info;
import static net.pcal.fastback.WorldUtils.getWorldUuid;
import static net.pcal.fastback.tasks.Task.TaskState.FAILED;

@SuppressWarnings("FieldCanBeLocal")
public class BackupTask extends Task {

    private final ModConfig modConfig;
    private final Path worldSaveDir;
    private final Loginator logger;

    public BackupTask(final ModConfig modConfig, final Path worldSaveDir, final Loginator logger) {
        this.worldSaveDir = requireNonNull(worldSaveDir);
        this.modConfig = requireNonNull(modConfig);
        this.logger = requireNonNull(logger);
    }

    public void run() {
        final String worldUuid;
        try {
            worldUuid = getWorldUuid(worldSaveDir);
        } catch (IOException e) {
            error(logger, "Local backup failed.  Could not determine world-uuid.", e);
            return;
        }
        final String newBranchName = createSnapshotBranchName(worldUuid, logger);
        info(logger, "Creating " + newBranchName);
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
