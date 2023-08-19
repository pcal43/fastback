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

public interface RepoConfig {

    @Deprecated
    static RepoConfig load(Git jgit) {
        return RepoConfigImpl.load(jgit);
    }

    boolean getBoolean(RepoConfigKey key);

    String getString(RepoConfigKey key);

    int getInt(RepoConfigKey key);

    Updater updater();

    interface Updater {

        Updater set(RepoConfigKey key, boolean newValue);

        Updater set(RepoConfigKey key, String newValue);

        Updater set(RepoConfigKey key, int newValue);

        void save() throws IOException;
    }
}