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
import net.pcal.fastback.mod.Mod;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static net.pcal.fastback.config.GitConfigKey.BROADCAST_NOTICE_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.BROADCAST_NOTICE_MESSAGE;
import static net.pcal.fastback.config.GitConfigKey.COMMIT_SIGNING_ENABLED;
import static net.pcal.fastback.logging.SystemLogger.syslog;

/**
 * @author pcal
 * @since 0.13.0
 */
class RepoFactoryImpl implements RepoFactory {

    @Override
    public Repo init(Path worldSaveDir, Mod mod) throws IOException {
        try (final Git jgit = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
             final Repo repo = new RepoImpl(jgit, mod);
             final Updater updater = repo.getConfig().updater();
             updater.set(COMMIT_SIGNING_ENABLED, false);
             updater.set(BROADCAST_NOTICE_ENABLED, true);
             updater.save();
             return repo;
        } catch (GitAPIException e) {
            syslog().error("Error initializing repo", e);
            throw new IOException(e);
        }
    }

    @Override
    public Repo load(Path worldSaveDir, Mod mod) throws IOException {
        final Git jgit = Git.open(worldSaveDir.toFile());
        return new RepoImpl(jgit, mod);
    }

    @Override
    public boolean isGitRepo(final Path worldSaveDir) {
        final File dotGit = worldSaveDir.resolve(".git").toFile();
        return dotGit.exists() && dotGit.isDirectory();
    }
}
