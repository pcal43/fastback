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

import static net.pcal.fastback.LogUtils.debug;
import static net.pcal.fastback.LogUtils.info;
import static net.pcal.fastback.ModConfig.Key.REPO_LATEST_BRANCH_NAME;
import static net.pcal.fastback.WorldUtils.getWorldInfo;

public class CommitUtils {

    public static void doCommit(final ModConfig modConfig, final Path worldSaveDir, String newBranchName, final Loginator logger) throws GitAPIException, IOException {
        final long startTime = System.currentTimeMillis();
        info(logger, "Starting local backup.");
        debug(logger, "doing commit");
        final Git git;
        debug(logger, "init");
        git = Git.init().setDirectory(worldSaveDir.toFile()).call();
        debug(logger, "checkout");
        git.checkout().setOrphan(true).setName(newBranchName).call();
        debug(logger, "status");
        final Status status = git.status().call();

        debug(logger, "add");

        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=494323
        //
        // One workaround would be to first fire a jgit status to find the modified and new files and then do a jgit add with explicit pathes. That should be fast.

        final AddCommand gitAdd = git.add();
        for (String file : status.getModified()) {
            debug(logger, ()->"add modified " + file);
            gitAdd.addFilepattern(file);
        }
        for (String file : status.getUntracked()) {
            gitAdd.addFilepattern(file);
            debug(logger, ()->"add untracked " + file);
        }
        debug(logger, "doing add");
        gitAdd.call();

        final Collection<String> toDelete = new ArrayList<>();
        toDelete.addAll(status.getRemoved());
        toDelete.addAll(status.getMissing());
        if (!toDelete.isEmpty()) {
            final RmCommand gitRm = git.rm();
            for (final String file : toDelete) {
                gitRm.addFilepattern(file);
                debug(logger, ()-> "removed "+file);
            }
            debug(logger, "dong rm");
            gitRm.call();
        }
        debug(logger, "commit");
        git.commit().setMessage(getWorldInfo(worldSaveDir)).call();
        final String latestBranchName = modConfig.get(REPO_LATEST_BRANCH_NAME);
        debug(logger, "Updating " + latestBranchName);
        git.branchCreate().setForce(true).setName(latestBranchName).call();
        final Duration duration = Duration.ofMillis(System.currentTimeMillis() - startTime).plusMillis(900);
        info(logger, "Local backup complete.  Elapsed time: " + duration.toMinutesPart() + "m " + duration.toSecondsPart() + "s");
    }
}
