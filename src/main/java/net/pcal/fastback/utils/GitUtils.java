/*
 * FastBack - Fast, incremental Minecraft backups powered by Git.
 * Copyright (C) 2022 pcal.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package net.pcal.fastback.utils;

import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

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

    @Deprecated
    public static String getBranchName(Ref fromBranchRef) {
        final String REFS_HEADS = "refs/heads/";
        final String name = fromBranchRef.getName();
        if (name.startsWith(REFS_HEADS)) {
            return name.substring(REFS_HEADS.length());
        } else {
            return null;
        }
    }

    @Deprecated
    public static String getRemoteBranchName(Ref fromBranchRef) {
        final String REFS_HEADS = "refs/heads/";
        final String name = fromBranchRef.getName();
        if (name.startsWith(REFS_HEADS)) {
            return name.substring(REFS_HEADS.length());
        } else {
            return null;
        }
    }

    public static URIish getRemoteUri(Git git, String remoteName, Logger logger) throws GitAPIException {
        requireNonNull(git);
        requireNonNull(remoteName);
        final List<RemoteConfig> remotes = git.remoteList().call();
        for (final RemoteConfig remote : remotes) {
            logger.debug("getRemoteUri " + remote);
            if (remote.getName().equals(remoteName)) {
                return remote.getPushURIs() != null && !remote.getURIs().isEmpty() ? remote.getURIs().get(0) : null;
            }
        }
        return null;
    }

    public static void deleteRemoteBranch(Git git, String remoteName, String remoteBranchName) throws GitAPIException {
        RefSpec refSpec = new RefSpec()
                .setSource(null)
                .setDestination("refs/heads/" + remoteBranchName);
        git.push().setRefSpecs(refSpec).setRemote(remoteName).call();
    }

    public static boolean isGitRepo(Path worldSaveDir) {
        final File dotGit = worldSaveDir.resolve(".git").toFile();
        return dotGit.exists() && dotGit.isDirectory();
    }

    public static String getFileUri(Path localPath) {
        return "file://" + localPath.toAbsolutePath();
    }
}
