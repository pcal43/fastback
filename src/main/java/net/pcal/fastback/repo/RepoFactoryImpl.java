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
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.utils.EnvironmentUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static net.pcal.fastback.config.FastbackConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.config.OtherConfigKey.COMMIT_SIGNING_ENABLED;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.*;
import static net.pcal.fastback.logging.UserMessage.raw;
import static net.pcal.fastback.logging.UserMessage.styledRaw;
import static net.pcal.fastback.repo.WorldIdUtils.createWorldId;
import static net.pcal.fastback.repo.WorldIdUtils.ensureWorldHasId;

/**
 * @author pcal
 * @since 0.13.0
 */
class RepoFactoryImpl implements RepoFactory {

    @Override
    public void doInit(final Path worldSaveDir, final UserLogger ulog) throws IOException {
        if (isGitRepo(worldSaveDir)) {
            ensureWorldHasId(worldSaveDir);
            ulog.message(styledRaw("Backups already initialized.", WARNING)); // FIXME i18n
            return;
        }
        try (final Git jgit = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
            createWorldId(worldSaveDir);
            Repo repo = new RepoImpl(jgit);
            final Updater updater = repo.getConfig().updater();
            updater.set(COMMIT_SIGNING_ENABLED, false); // because some people have it set globally
            if (EnvironmentUtils.isNativeGitInstalled()) {
                ulog.message(styledRaw("Native git detected.", NATIVE_GIT)); // FIXME i18n
                updater.set(IS_NATIVE_GIT_ENABLED, true);
            } else {
                ulog.message(styledRaw("Native git not installed on your system.", WARNING)); // FIXME i18n
                ulog.message(raw("Native git is not required but it makes Fastback *much* faster.  You are strongly encouraged to install it *before* doing your first backup."));
                ulog.message(raw("For more information, see https://pcal43.github.io/fastback/native-git.html"));
                updater.set(IS_NATIVE_GIT_ENABLED, false);
            }
            updater.save();
            ulog.message(raw("Backups initialized.  Run '/backup local' to do your first backup.  '/backup help' for more options."));
        } catch (GitAPIException e) {
            syslog().error("Error initializing repo", e);
            throw new IOException(e);
        }
    }

    @Override
    public boolean doInitCheck(Path worldSaveDir, UserLogger ulog) {
        if (!isGitRepo(worldSaveDir)) {
            ulog.message(styledRaw("Please run '/backup init' first.", ERROR));
            return false;
        }
        return true;
    }

    @Override
    public Repo load(final Path worldSaveDir) throws IOException {
        final Git jgit = Git.open(worldSaveDir.toFile());
        // It should already be there.  But let's try to be extra sure this is there, because lots of stuff
        // will blow up if it's missing.
        ensureWorldHasId(worldSaveDir);
        return new RepoImpl(jgit);
    }

    @Override
    public boolean isGitRepo(final Path worldSaveDir) {
        final File dotGit = worldSaveDir.resolve(".git").toFile();
        return dotGit.exists() && dotGit.isDirectory();
    }
}
