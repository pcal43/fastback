package net.pcal.fastback.tasks;

import net.pcal.fastback.Loggr;
import net.pcal.fastback.WorldConfig;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.BranchNameUtils.createNewSnapshotBranchName;
import static net.pcal.fastback.BranchNameUtils.getLatestBranchName;

@SuppressWarnings("FieldCanBeLocal")
public class BackupTask extends Task {

    private final Path worldSaveDir;
    private final TaskListener listener;
    private final Loggr logger;

    public BackupTask(final Path worldSaveDir, TaskListener listener, final Loggr logger) {
        this.worldSaveDir = requireNonNull(worldSaveDir);
        this.listener = requireNonNull(listener);
        this.logger = requireNonNull(logger);
    }

    public void run() {
        this.setStarted();
        try (Git git = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
            final WorldConfig config;
            try {
                config = WorldConfig.load(worldSaveDir, git.getRepository().getConfig());
            } catch (IOException e) {
                listener.internalError();
                logger.error("Local backup failed.  Could not determine world-uuid.", e);
                this.setFailed();
                return;
            }
            final String newBranchName = createNewSnapshotBranchName(config.worldUuid());
            logger.info("Committing " + newBranchName);
            try {
                doCommit(git, config, newBranchName, logger);
                final Duration dur = getSplitDuration();
                logger.info("Local backup complete.  Elapsed time: " + dur.toMinutesPart() + "m " + dur.toSecondsPart() + "s");
            } catch (GitAPIException | IOException e) {
                listener.internalError();
                logger.error("Local backup failed.  Unable to commit changes.", e);
                this.setFailed();
                return;
            }

            if (config.isRemoteBackupEnabled()) {
                final PushTask push = new PushTask(worldSaveDir, newBranchName, this.listener, logger);
                push.run();
                final Duration dur = getSplitDuration();
                logger.info("Remote backup complete.  Elapsed time: " + dur.toMinutesPart() + "m " + dur.toSecondsPart() + "s");
                if (push.isFailed()) {
                    logger.error("Local backup succeeded but remote backup failed.");
                }
            } else {
                logger.info("Remote backup disabled in config.");
            }
        } catch (GitAPIException e) {
            listener.internalError();
            logger.error(e);
            this.setFailed();
            return;
        }
        this.listener.feedback("Backup complete.");
        this.setCompleted();
    }

    private static void doCommit(Git git, WorldConfig config, String newBranchName, final Loggr logger) throws GitAPIException, IOException {
        logger.info("Starting local backup.");
        logger.debug("doing commit");
        logger.debug("checkout");
        git.checkout().setOrphan(true).setName(newBranchName).call();
        git.reset().setMode(ResetCommand.ResetType.SOFT).call();
        logger.debug("status");
        final Status status = git.status().call();

        logger.debug("add");

        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=494323
        //
        // One workaround would be to first fire a jgit status to find the modified and new files and then do a jgit add with explicit patches. That should be fast.

        final AddCommand gitAdd = git.add();
        for (String file : status.getModified()) {
            logger.debug(() -> "add modified " + file);
            gitAdd.addFilepattern(file);
        }
        for (String file : status.getUntracked()) {
            gitAdd.addFilepattern(file);
            logger.debug(() -> "add untracked " + file);
        }
        logger.debug("doing add");
        gitAdd.call();

        final Collection<String> toDelete = new ArrayList<>();
        toDelete.addAll(status.getRemoved());
        toDelete.addAll(status.getMissing());
        if (!toDelete.isEmpty()) {
            final RmCommand gitRm = git.rm();
            for (final String file : toDelete) {
                gitRm.addFilepattern(file);
                logger.debug(() -> "removed " + file);
            }
            logger.debug("doing rm");
            gitRm.call();
        }
        logger.debug("commit");
        git.commit().setMessage(newBranchName).call();
        final String latestBranchName = getLatestBranchName(config.worldUuid());
        logger.debug("Updating " + latestBranchName);
        git.branchCreate().setForce(true).setName(latestBranchName).call();
    }
}
