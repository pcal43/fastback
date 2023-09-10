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
import net.pcal.fastback.logging.UserMessage.UserMessageStyle;
import net.pcal.fastback.utils.FileUtils;
import net.pcal.fastback.utils.ProcessUtils;
import net.pcal.fastback.utils.ProcessException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.config.FastbackConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.RESTORE_DIRECTORY;
import static net.pcal.fastback.config.OtherConfigKey.REMOTE_PUSH_URL;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.NATIVE_GIT;
import static net.pcal.fastback.logging.UserMessage.localized;
import static net.pcal.fastback.logging.UserMessage.styledLocalized;
import static net.pcal.fastback.logging.UserMessage.styledRaw;
import static net.pcal.fastback.mod.Mod.mod;

/**
 * Utilities for restoring a snapshot
 *
 * @author pcal
 * @since 0.13.0
 */
abstract class RestoreUtils {

    // ======================================================================
    // Package private

    static void doRestoreLocalSnapshot(final String snapshotNameToRestore, final RepoImpl repo, final UserLogger ulog) {
        doRestoreSnapshot(snapshotNameToRestore, "file://" + mod().getWorldDirectory().toAbsolutePath(), repo, ulog);
    }

    static void doRestoreRemoteSnapshot(final String snapshotNameToRestore, final RepoImpl repo, final UserLogger ulog) {
        final GitConfig conf = repo.getConfig();
        if (!conf.isSet(REMOTE_PUSH_URL)) {
            ulog.message(styledLocalized("fastback.chat.remote-no-url", ERROR));
        } else {
            doRestoreSnapshot(snapshotNameToRestore, conf.getString(REMOTE_PUSH_URL), repo, ulog);
        }
    }

    // ======================================================================
    // Private

    private static void doRestoreSnapshot(final String snapshotNameToRestore, final String repoUri, final RepoImpl repo, final UserLogger ulog) {
        try {
            final GitConfig conf = repo.getConfig();
            final SnapshotId sid = repo.createSnapshotId(snapshotNameToRestore);
            final Path allRestoresDir = conf.isSet(RESTORE_DIRECTORY) ?
                    Paths.get(conf.getString(RESTORE_DIRECTORY)) : mod().getDefaultRestoresDir();
            final Path restoreTargetDir = getTargetDir(allRestoresDir, mod().getWorldName(), sid.getShortName());
            if (conf.getBoolean(IS_NATIVE_GIT_ENABLED)) {
                native_restoreSnapshot(sid.getBranchName(), restoreTargetDir, repoUri, ulog);
            } else {
                jgit_restoreSnapshot(sid.getBranchName(), restoreTargetDir, repoUri, ulog);
            }
            ulog.message(localized("fastback.chat.restore-done", restoreTargetDir));
        } catch (Exception e) {
            syslog().error(e);
            ulog.message(styledRaw("Restore failed.  See log for details.", ERROR)); // FIXME i18n
        }
    }

    private static void native_restoreSnapshot(final String branchName, final Path restoreTargetDir, final String repoUri, final UserLogger ulog) throws ProcessException {
        final Map<String, String> env = Map.of("GIT_LFS_FORCE_PROGRESS", "1");
        final Consumer<String> outputConsumer = line -> ulog.update(styledRaw(line, NATIVE_GIT));
        final String restoreTargetDirStr = restoreTargetDir.toString();
        syslog().debug("Cloning repo at " + repoUri);
        ProcessUtils.doExec(new String[]{
                "git", "clone", repoUri, "--no-checkout", "--branch", branchName, "--single-branch", restoreTargetDirStr
        }, env, outputConsumer, outputConsumer);
        syslog().debug("Installing lfs locally in " + restoreTargetDirStr);
        ProcessUtils.doExec(new String[]{
                "git", "-C", restoreTargetDirStr, "lfs", "install", "--local"
        }, env, outputConsumer, outputConsumer);
        syslog().debug("Checking out " + branchName + ", downloading lfs blobs");
        ProcessUtils.doExec(new String[]{
                "git", "-C", restoreTargetDirStr, "checkout", branchName
        }, env, outputConsumer, outputConsumer);
    }

    private static void jgit_restoreSnapshot(final String branchName, final Path restoreTargetDir, final String repoUri, final UserLogger ulog) throws IOException, GitAPIException {
        ulog.update(localized("fastback.hud.restore-percent", 0));
        final ProgressMonitor pm = new JGitIncrementalProgressMonitor(new JGitRestoreProgressMonitor(ulog), 100);
        try (Git git = Git.cloneRepository().setProgressMonitor(pm).setDirectory(restoreTargetDir.toFile()).
                setBranchesToClone(List.of("refs/heads/" + branchName)).setBranch(branchName).setURI(repoUri).call()) {
        }
        FileUtils.rmdir(restoreTargetDir.resolve(".git"));
    }

    /**
     * @param allRestoresDir - general location for restorations to go.  e.g., the 'saves' dir by default if client
     * @param worldName      - name of the world
     * @param snapshotName   - name of the snapshot being restored
     * @return The absolute path to the directory where the snapshot should be restored
     */
    private static Path getTargetDir(Path allRestoresDir, String worldName, String snapshotName) {
        worldName = worldName.replaceAll("\\W+", ""); // strip out all non-word characters for safety
        Path base = allRestoresDir.resolve(worldName + "-" + snapshotName);
        Path candidate = base;
        int i = 0;
        while (candidate.toFile().exists()) {
            i++;
            candidate = Path.of(base + "_" + i);
            if (i > 1000) {
                throw new IllegalStateException("wat i = " + i);
            }
        }
        return candidate;
    }

    private static class JGitRestoreProgressMonitor extends JGitPercentageProgressMonitor {

        private final UserLogger ulog;

        public JGitRestoreProgressMonitor(UserLogger ulog) {
            this.ulog = requireNonNull(ulog);
        }

        @Override
        public void progressStart(String task) {
        }
        //remote: Finding sources
        //Receiving objects
        //Updating references
        //Checking out files   %

        @Override
        public void progressUpdate(String task, int percentage) {
            final String message = task + " " + percentage + "%";
            syslog().debug(message);
            this.ulog.update(styledRaw(message, UserMessageStyle.JGIT));
        }

        @Override
        public void progressDone(String task) {
        }

        @Override
        public void showDuration(boolean enabled) {
        }

    }
}
