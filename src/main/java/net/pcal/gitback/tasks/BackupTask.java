package net.pcal.gitback.tasks;

import net.pcal.gitback.WorldConfig;
import net.pcal.gitback.logging.Logger;
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
import static net.pcal.gitback.BranchNameUtils.createNewSnapshotBranchName;
import static net.pcal.gitback.BranchNameUtils.getLatestBranchName;

@SuppressWarnings("FieldCanBeLocal")
public class BackupTask extends Task {

    private final Path worldSaveDir;
    private final Logger log;

    public BackupTask(final Path worldSaveDir, final Logger log) {
        this.worldSaveDir = requireNonNull(worldSaveDir);
        this.log = requireNonNull(log);
    }

    public void run() {
        this.setStarted();
        this.log.notify("Saving local backup");
        try (Git git = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
            final WorldConfig config;
            try {
                config = WorldConfig.load(worldSaveDir, git.getRepository().getConfig());
            } catch (IOException e) {
                log.internalError("Local backup failed.  Could not determine world-uuid.", e);
                this.setFailed();
                return;
            }
            final String newBranchName = createNewSnapshotBranchName(config.worldUuid());
            log.info("Committing " + newBranchName);
            try {
                doCommit(git, config, newBranchName, log);
                final Duration dur = getSplitDuration();
                log.info("Local backup complete.  Elapsed time: " + dur.toMinutesPart() + "m " + dur.toSecondsPart() + "s");
                this.log.notify("Local backup complete.");
            } catch (GitAPIException | IOException e) {
                log.internalError("Local backup failed.  Unable to commit changes.", e);
                this.setFailed();
                return;
            }
            if (config.isRemoteBackupEnabled()) {
                this.log.notify("Starting remote backup...");
                final PushTask push = new PushTask(worldSaveDir, newBranchName, log);
                push.run();
                if (push.isFailed()) {
                    log.notifyError("Local backup succeeded but remote backup failed.  See log for details.");
                } else {
                    final Duration dur = getSplitDuration();
                    log.notify("Remote backup to complete");
                    log.info("Elapsed time: " + dur.toMinutesPart() + "m " + dur.toSecondsPart() + "s");
                }
            } else {
                log.info("Remote backup disabled.");
            }
        } catch (GitAPIException e) {
            log.internalError("Backup failed unexpectedly", e);
            this.setFailed();
            return;
        }
        this.setCompleted();
    }

    private static void doCommit(Git git, WorldConfig config, String newBranchName, final Logger logger) throws GitAPIException, IOException {
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
            logger.debug("add modified " + file);
            gitAdd.addFilepattern(file);
        }
        for (String file : status.getUntracked()) {
            gitAdd.addFilepattern(file);
            logger.debug("add untracked " + file);
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
                logger.debug("removed " + file);
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
