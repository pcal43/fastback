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

package net.pcal.fastback.common.mod;

import java.util.Map;

/**
 * Abstracts away loader/environment-specific services that the mod framework (e.g. Fabric)
 * must provide. Implemented by loader-specific classes; has no Minecraft server or client deps.
 *
 * @author pcal
 * @since 0.2.0
 */
public interface LoaderHelper {

    /** The mod id, shared across all loaders. */
    String MOD_ID = "fastback";

    /** @return the version string of the fastback mod as reported by the loader. */
    String getModVersion();

    /**
     * Appends loader-specific properties (e.g. the installed mod list) to the backup
     * properties map. Common minecraft-* and fastback-version entries are added by ModImpl.
     */
    void addLoaderBackupProperties(Map<String, String> props);
}

