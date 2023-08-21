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

import com.google.common.collect.ListMultimap;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.Callable;

/**
 * @author pcal
 */
public interface Repo extends AutoCloseable {

    GitConfig getConfig();

    String getWorldUuid() throws IOException;

    File getDirectory() throws NoWorkTreeException;

    File getWorkTree() throws NoWorkTreeException;

    void doCommitAndPush() throws IOException;

    void doCommitSnapshot() throws IOException;

    Callable<Collection<SnapshotId>> createLocalPruneTask();

    Callable<Void> createGcTask();

    Callable<Collection<SnapshotId>> createRemotePruneTask();

    Callable<Path> restoreSnapshotTask(String uri, Path restoresDir, String worldName, SnapshotId sid, Logger log) throws IOException;

    ListMultimap<String, SnapshotId> listSnapshots() throws IOException;

    ListMultimap<String, SnapshotId> listRemoteSnapshots() throws IOException;

    void deleteRemoteBranch(String remoteName, String remoteBranchName) throws IOException;

    void deleteBranch(String branchName) throws GitAPIException;

    void doWorldMaintenance(Logger logger) throws IOException, IOException;
}
