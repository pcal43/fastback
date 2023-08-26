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

package net.pcal.fastback.repo;

import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.config.GitConfigKey;
import net.pcal.fastback.logging.UserLogger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Encapsulates everything the mod needs to do to the git repo.
 *
 * @author pcal
 * @since 0.13.0
 */
public interface Repo extends AutoCloseable {

    GitConfig getConfig();

    /**
     * @return the UUID of the world.
     * @throws java.io.FileNotFoundException if the world.id file is missing for some reason.
     */
    WorldId getWorldId() throws IOException;

    File getDirectory() throws NoWorkTreeException;

    File getWorkTree() throws NoWorkTreeException;

    Set<SnapshotId> getLocalSnapshots() throws IOException;

    Set<SnapshotId> getRemoteSnapshots() throws IOException;

    void doCommitAndPush(UserLogger ulog) throws IOException;

    void doCommitSnapshot(UserLogger ulog) throws IOException;

    Collection<SnapshotId> doLocalPrune(UserLogger ulog) throws IOException;

    Collection<SnapshotId> doRemotePrune(UserLogger ulog) throws IOException;

    void doGc(UserLogger ulog) throws IOException;

    Path doRestoreSnapshot(String uri, Path restoresDir, String worldName, SnapshotId sid, UserLogger ulog) throws IOException;

    void deleteRemoteBranch(String remoteBranchName) throws IOException;

    void deleteLocalBranches(List<String> branchesToDelete) throws GitAPIException, IOException;

    void doPushSnapshot(SnapshotId sid, UserLogger ulog) throws IOException, ParseException;

    void setConfigValue(GitConfigKey key, boolean value, UserLogger userlog);

    Path getRestoresDir() throws IOException;

    SnapshotId createSnapshotId(String date) throws IOException, ParseException;
}
