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

package net.pcal.fastback.config;

import org.eclipse.jgit.api.Git;

import java.io.IOException;

/**
 * Abstract representation of a git worktree's configuration.
 *
 * @author pcal
 */
public interface GitConfig {

    // @Deprecated - eventually we want the public contract to stop being
    // coupled to jgit
    static GitConfig load(Git jgit) {
        return GitConfigImpl.load(jgit);
    }

    boolean getBoolean(GitConfigKey key);

    String getString(GitConfigKey key);

    int getInt(GitConfigKey key);

    Updater updater();

    /**
     * Helper for updating the local .git/config file.
     */
    interface Updater {

        Updater set(GitConfigKey key, boolean newValue);

        Updater set(GitConfigKey key, String newValue);

        Updater set(GitConfigKey key, int newValue);

        Updater setCommented(GitConfigKey key, boolean newValue);

        Updater setCommented(GitConfigKey key, String newValue);

        Updater setCommented(GitConfigKey key, int newValue);

        void save() throws IOException;
    }
}