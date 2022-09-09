package net.pcal.gitback;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class GitUtils {

//    public static Git createTempRepo(Git original, Logger logger) throws IOException, GitAPIException {
//        final Path tempDir = Files.createTempDirectory("fastback-temp");
//        debug(logger, "Creating temp repo at "+tempDir);
//        tempDir.toFile().mkdirs();
//        final Git tempGit = Git.init().setDirectory(tempDir.toFile()).call();
//        final String originalConfig = original.getRepository().getConfig().toText();
//        debug(logger, "originalConfig = "+originalConfig);
//        try {
//            tempGit.getRepository().getConfig().fromText(originalConfig);
//        } catch (ConfigInvalidException e) {
//            throw new InvalidConfigurationException("originalConfig = "+originalConfig);
//        }
//        return tempGit;
//    }


    public static Set<String> getRemoteBranchNames(Git git, String remoteName, Loggr logger) throws GitAPIException {
        final String UUID_REFNAME_PREFIX = "refs/heads/";
        final Collection<Ref> refs = git.lsRemote().setHeads(true).setTags(false).setRemote(remoteName).call();
        final Set<String> branchNames = new HashSet<>();
        for (final Ref ref : refs) {
            logger.debug("ls-remote  " + ref);
            branchNames.add(ref.getName().substring(UUID_REFNAME_PREFIX.length()));
        }
        return branchNames;
    }

    public static URIish getRemoteUri(Git git, String remoteName, Loggr logger) throws GitAPIException {
        requireNonNull(git);
        requireNonNull(remoteName);
        final List<RemoteConfig> remotes = git.remoteList().call();
        for (final RemoteConfig remote : remotes) {
            logger.trace("getRemoteUri " + remote);
            if (remote.getName().equals(remoteName)) {
                return remote.getPushURIs() != null && !remote.getURIs().isEmpty() ? remote.getURIs().get(0) : null;
            }
        }
        return null;
    }

    public static boolean isBranchExtant(Git git, String name, Loggr logger) throws GitAPIException {
        try {
            git.branchList().setContains(name).call();
            return true;
        } catch (RefNotFoundException e) {
            logger.trace(() -> name + " doesn't exist", e);
            return false;
        }
    }

    public static boolean isGitRepo(Path worldSaveDir) {
        final File dotGit = worldSaveDir.resolve(".git").toFile();
        return dotGit.exists() && dotGit.isDirectory();
    }

}
