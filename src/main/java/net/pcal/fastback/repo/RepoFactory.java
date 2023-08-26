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

import net.pcal.fastback.logging.UserLogger;

import java.io.IOException;
import java.nio.file.Path;

import static net.pcal.fastback.mod.Mod.mod;

/**
 * Creates Repo instances.
 *
 * @author pcal
 * @since 0.13.0
 */
public interface RepoFactory {

    // TODO this probably should move to ModContext
    static RepoFactory rf() {
        return new RepoFactoryImpl();
    }

    void doInit(Path worldSaveDir, UserLogger ulog) throws IOException;

    Repo load(final Path worldSaveDir) throws IOException;

    boolean doInitCheck(Path worldSaveDir, UserLogger ulog);

    boolean isGitRepo(Path worldSaveDir);

}
