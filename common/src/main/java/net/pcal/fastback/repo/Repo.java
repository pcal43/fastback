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
import net.pcal.fastback.logging.UserLogger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;

import java.io.File;
import java.io.IOException;
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


    Set<SnapshotId> getLocalSnapshots() throws IOException;

    Set<SnapshotId> getRemoteSnapshots() throws IOException;

    // ======================================================================
    // 'do' methods.
    //
    // By convention, methods prefixed with 'do' provide the 'guts' of a flow
    // initiated by a cli command or scheduled action.  They're expected to handle
    // everything: errors, user feedback.  A method prefixed with 'do' must return
    // void and must not throw checked exceptions.
    //
    // q: should they also be responsible for thread management?  probably yes
    // Obviously there are still some TODOs here to align with this convention.
    //

    void doCommitAndPush(UserLogger ulog) throws IOException;

    void doCommitSnapshot(UserLogger ulog) throws IOException;

    Collection<SnapshotId> doLocalPrune(UserLogger ulog) throws IOException;

    Collection<SnapshotId> doRemotePrune(UserLogger ulog) throws IOException;

    void doRestoreLocalSnapshot(String snapshotName, UserLogger ulog);

    void doRestoreRemoteSnapshot(String snapshotName, UserLogger ulog);

    void doGc(UserLogger ulog);

    void doPushSnapshot(SnapshotId sid, UserLogger ulog);

    void deleteRemoteBranch(String remoteBranchName) throws IOException;

    void deleteLocalBranches(List<String> branchesToDelete) throws GitAPIException, IOException;


    // ======================================================================
    // Any callers of these methods are doing too much; they need to be given a
    // 'do' method instead

    @Deprecated
    SnapshotId createSnapshotId(String date) throws IOException, ParseException;

    @Deprecated
    GitConfig getConfig();

    @Deprecated
    WorldId getWorldId() throws IOException;

    @Deprecated
    File getDirectory() throws NoWorkTreeException;

    @Deprecated
    File getWorkTree() throws NoWorkTreeException;

}
