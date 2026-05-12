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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.pcal.fastback.common.commands.PermissionsFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Abstracts away loader/environment-specific services that the mod framework
 * (e.g. Fabric) must provide.
 *
 * @author pcal
 * @since 0.2.0
 */
public interface LoaderHelper {

    /**
     * @return the version string of the fastback mod as reported by the loader.
     */
    String getModVersion();

    /**
     * Appends loader-specific properties (e.g. the installed mod list) to the backup
     * properties map. Common minecraft-* and fastback-version entries are added by ModImpl.
     */
    void addLoaderBackupProperties(Map<String, String> props);

    /**
     * @return path to the client 'saves' directory, or null on a dedicated server.
     */
    Path getSavesDir();

    /**
     * @return paths that should be included when mods-backup is enabled.
     */
    Collection<Path> getModsBackupPaths();

    /**
     * Create the /backup command using the given builder and register it.
     *
     * @param isClient true when running on an integrated (client-embedded) server.
     */
    void registerBackupCommand(boolean isClient,
                               Function<PermissionsFactory<CommandSourceStack>, LiteralArgumentBuilder<CommandSourceStack>> builder);
}
