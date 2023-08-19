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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class RepoConfigImpl implements RepoConfig {

    public static RepoConfig load(final Path worldSaveDir) throws IOException {
        return load(Git.open(worldSaveDir.toFile()))
    }

    static RepoConfig load(final Git jgit) {
        return new RepoConfigImpl(jgit.getRepository().getConfig());
    }

    private final StoredConfig storedConfig;

    RepoConfigImpl(StoredConfig jgitConfig) {
        this.storedConfig = requireNonNull(jgitConfig);
    }

    @Override
    public boolean getBoolean(RepoConfigKey key) {
        return storedConfig.getBoolean(key.getSectionName(), key.getSubSectionName(), key.getSettingName(), key.getBooleanDefault());
    }

    @Override
    public String getString(RepoConfigKey key) {
        final String out = storedConfig.getString(key.getSectionName(), key.getSubSectionName(), key.getSettingName());
        return out != null ? out : key.getStringDefault();
    }

    @Override
    public int getInt(RepoConfigKey key) {
        return storedConfig.getInt(key.getSectionName(), key.getSubSectionName(), key.getSettingName(), key.getIntDefault());
    }

    class UpdaterImpl implements Updater {

        @Override
        public Updater set(RepoConfigKey key, boolean newValue) {
            storedConfig.
            return this;
        }

        @Override
        public Updater set(RepoConfigKey key, String newValue) {
            return this;
        }

        @Override
        public Updater set(RepoConfigKey key, int newValue) {
            return this;
        }

        @Override
        public void save() throws IOException {

        }
    }
}
