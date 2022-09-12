package net.pcal.fastback.tasks;

import net.pcal.fastback.FileUtils;
import net.pcal.fastback.GitUtils;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.IncrementalProgressMonitor;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.LoggingProgressMonitor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.BranchNameUtils.getSnapshotBranchName;
import static net.pcal.fastback.WorldConfig.WORLD_UUID_PATH;

@SuppressWarnings("FieldCanBeLocal")
public class RestoreSnapshotTask extends Task {

    private final Path worldSaveDir;
    private final String snapshotName;
    private final String worldName;
    private final Path saveDir;
    private final boolean doClean = true;
    private final Logger logger;

    public static Runnable create(Path worldSaveDir,
                                  String snapshotName,
                                  String worldName,
                                  Path saveDir,
                                  Logger logger) {
        return new RestoreSnapshotTask(worldSaveDir, snapshotName, worldName, saveDir, logger);
    }

    private RestoreSnapshotTask(Path worldSaveDir, String snapshotName, String worldName, Path saveDir, Logger logger) {
        this.worldSaveDir = requireNonNull(worldSaveDir);
        this.snapshotName = requireNonNull(snapshotName);
        this.worldName = requireNonNull(worldName);
        this.saveDir = requireNonNull(saveDir);
        this.logger = requireNonNull(logger);
    }

    public void run() {
        setStarted();
        final WorldConfig config;
        final String branchName;
        try (final Git git = Git.open(this.worldSaveDir.toFile())) {
            config = WorldConfig.load(worldSaveDir, git.getRepository().getConfig());
            branchName = getSnapshotBranchName(config.worldUuid(), this.snapshotName);
            if (!GitUtils.isBranchExtant(git, branchName, logger)) {
                logger.notifyError("No such snapshot " + snapshotName);
                return;
            }
        } catch (IOException | GitAPIException e) {
            logger.internalError("Unexpected error looking up branch names", e);
            setFailed();
            return;
        }

        final Path targetDirectory;
        try {
            targetDirectory = getTargetDir(this.saveDir, worldName, snapshotName);
            String uri = "file://" + this.worldSaveDir.toAbsolutePath();
            logger.notify("Restoring " + this.snapshotName + " to\n" + targetDirectory);
            final ProgressMonitor pm = new IncrementalProgressMonitor(new LoggingProgressMonitor(logger), 100);
            try (Git git = Git.cloneRepository().setProgressMonitor(pm).setDirectory(targetDirectory.toFile()).
                    setBranchesToClone(List.of("refs/heads/" + branchName)).setBranch(branchName).setURI(uri).call()) {

            }
        } catch (Exception e) {
            logger.internalError("Restoration clone of " + branchName + " failed.", e);
            setFailed();
            return;
        }
        if (config.isPostRestoreCleanupEnabled()) {
            try {
                FileUtils.rmdir(targetDirectory.resolve(".git"));
                targetDirectory.resolve(WORLD_UUID_PATH).toFile().delete();
            } catch (IOException e) {
                logger.notifyError("Restoration finished but an unexpected error " +
                        "occurred during cleanup.");
                logger.internalError("Unexpected error cleaning restored snapshot", e);
                setFailed();
                return;
            }
        }
        logger.notify("Restoration complete");
        setCompleted();
    }

    private static Path getTargetDir(Path saveDir, String worldName, String snapshotName) {
        worldName = worldName.replaceAll("\\W+", ""); // strip out all non-word characters for safety
        Path base = saveDir.resolve(worldName + "-" + snapshotName);
        Path candidate = base;
        int i = 0;
        while (candidate.toFile().exists()) {
            i++;
            candidate = Path.of(base + "_" + i);
            if (i > 1000) {
                throw new IllegalStateException("wat i = " + i);
            }
        }
        return candidate;

    }
}
