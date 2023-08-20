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

import net.pcal.fastback.ModContext;
import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

class RepoFactoryImpl implements RepoFactory {

    @Override
    public Repo init(Path worldSaveDir, ModContext mod, Logger log) throws IOException {
        try (final Git jgit = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
            final Repo repo = new RepoImpl(jgit, mod, log);
            repo.doWorldMaintenance(log);
            return repo;
        } catch (IOException ioe) {
            log.internalError("Error initializing repo", ioe);
            throw ioe;
        } catch (GitAPIException e) {
            log.internalError("Error initializing repo", e);
            throw new IOException(e);
        }
    }

    @Override
    public Repo load(Path worldSaveDir, ModContext mod, Logger log) throws IOException {
        final Git jgit = Git.open(worldSaveDir.toFile());
        //final GitConfig conf = GitConfig.load(jgit);
        return new RepoImpl(jgit, mod, log);
    }

    @Override
    public boolean isGitRepo(Path worldSaveDir) {
        final File dotGit = worldSaveDir.resolve(".git").toFile();
        return dotGit.exists() && dotGit.isDirectory();
    }
}
