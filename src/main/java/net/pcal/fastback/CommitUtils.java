package net.pcal.fastback;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

import static net.pcal.fastback.ModConfig.Key.REPO_LATEST_BRANCH_NAME;
import static net.pcal.fastback.WorldUtils.getWorldInfo;

public class CommitUtils {

    public static void doCommit(final ModConfig modConfig, final Path worldSaveDir, String newBranchName, final Loggr logger) throws GitAPIException, IOException {
        final long startTime = System.currentTimeMillis();
        logger.info("Starting local backup.");
        logger.debug("doing commit");
        final Git git;
        logger.debug("init");
        git = Git.init().setDirectory(worldSaveDir.toFile()).call();
        logger.debug("checkout");
        git.checkout().setOrphan(true).setName(newBranchName).call();
        logger.debug("status");
        final Status status = git.status().call();

        logger.debug("add");

        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=494323
        //
        // One workaround would be to first fire a jgit status to find the modified and new files and then do a jgit add with explicit pathes. That should be fast.

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
            logger.debug("dong rm");
            gitRm.call();
        }
        logger.debug("commit");
        git.commit().setMessage(getWorldInfo(worldSaveDir)).call();
        final String latestBranchName = modConfig.get(REPO_LATEST_BRANCH_NAME);
        logger.debug("Updating " + latestBranchName);
        git.branchCreate().setForce(true).setName(latestBranchName).call();
        final Duration duration = Duration.ofMillis(System.currentTimeMillis() - startTime).plusMillis(900);
        logger.info("Local backup complete.  Elapsed time: " + duration.toMinutesPart() + "m " + duration.toSecondsPart() + "s");
    }
}
