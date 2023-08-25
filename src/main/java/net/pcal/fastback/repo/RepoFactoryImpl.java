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

import net.pcal.fastback.config.GitConfig.Updater;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static net.pcal.fastback.config.FastbackConfigKey.BROADCAST_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.BROADCAST_MESSAGE;
import static net.pcal.fastback.config.FastbackConfigKey.IS_LOCK_CLEANUP_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.RESTORE_DIRECTORY;
import static net.pcal.fastback.config.OtherConfigKey.COMMIT_SIGNING_ENABLED;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.repo.WorldIdUtils.createWorldUuid;
import static net.pcal.fastback.repo.WorldIdUtils.ensureWorldHasUuid;

/**
 * @author pcal
 * @since 0.13.0
 */
class RepoFactoryImpl implements RepoFactory {

    @Override
    public Repo init(final Path worldSaveDir) throws IOException {
        try (final Git jgit = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
            createWorldUuid(worldSaveDir);
            final Repo repo = new RepoImpl(jgit);
            final Updater updater = repo.getConfig().updater();
            updater.set(COMMIT_SIGNING_ENABLED, false);
            updater.setCommented(IS_LOCK_CLEANUP_ENABLED, true);
            updater.setCommented(BROADCAST_ENABLED, true);
            updater.setCommented(BROADCAST_MESSAGE, "Attention: the server is starting a backup.");
            updater.setCommented(RESTORE_DIRECTORY, "/home/myuser/target/directory/for/restores");
            updater.save();
            return repo;
        } catch (GitAPIException e) {
            syslog().error("Error initializing repo", e);
            throw new IOException(e);
        }
    }

    @Override
    public Repo load(final Path worldSaveDir) throws IOException {
        final Git jgit = Git.open(worldSaveDir.toFile());
        // It should already be there.  But let's try to be extra sure this is there, because lots of stuff
        // will blow up if it's missing.
        ensureWorldHasUuid(worldSaveDir);
        return new RepoImpl(jgit);
    }

    @Override
    public boolean isGitRepo(final Path worldSaveDir) {
        final File dotGit = worldSaveDir.resolve(".git").toFile();
        return dotGit.exists() && dotGit.isDirectory();
    }
}
