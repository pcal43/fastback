package net.pcal.fastback.tasks;

import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.SnapshotId;
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
            final SnapshotId newSid = SnapshotId.create(config.worldUuid());
            final String newBranchName = newSid.getBranchName();
            log.info("Committing " + newBranchName);
            try {
                doCommit(git, config, newBranchName, log);
                final Duration dur = getSplitDuration();
                log.info("Local backup complete.  Elapsed time: " + dur.toMinutesPart() + "m " + dur.toSecondsPart() + "s");
                this.log.notify("Local backup complete");
            } catch (GitAPIException | IOException e) {
                log.internalError("Local backup failed.  Unable to commit changes.", e);
                this.setFailed();
                return;
            }
            if (config.isRemoteBackupEnabled()) {
                this.log.notify("Starting remote backup");
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

        //
        // Figure out what files to add and remove.  We don't just 'git add .' because this:
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=494323
        //
        {
            final Collection<String> toAdd = new ArrayList<>();
            toAdd.addAll(status.getModified());
            toAdd.addAll(status.getUntracked());
            if (!toAdd.isEmpty()) {
                final AddCommand gitAdd = git.add();
                logger.debug("doing add");
                for (final String file : toAdd) {
                    logger.debug("add  " + file);
                    gitAdd.addFilepattern(file);
                }
                gitAdd.call();
            }
        }
        {
            final Collection<String> toDelete = new ArrayList<>();
            toDelete.addAll(status.getRemoved());
            toDelete.addAll(status.getMissing());
            if (!toDelete.isEmpty()) {
                logger.debug("doing rm");
                final RmCommand gitRm = git.rm();
                for (final String file : toDelete) {
                    logger.debug("rm  " + file);
                    gitRm.addFilepattern(file);
                }
                gitRm.call();
            }
        }
        logger.debug("commit");
        git.commit().setMessage(newBranchName).call();
    }
}
