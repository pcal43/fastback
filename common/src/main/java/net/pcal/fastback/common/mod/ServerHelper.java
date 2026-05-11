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

import net.minecraft.server.MinecraftServer;

/**
 * Provides access to the current MinecraftServer instance. Implemented by both
 * client and dedicated-server loader classes; has no Fabric API deps.
 *
 * @author pcal
 * @since 0.2.0
 */
public interface ServerHelper {

    /**
     * Called by the loader initializer when a server starts or stops.
     *
     * @param serverOrNull the server that just started, or null when it stops.
     */
    void setMinecraftServer(MinecraftServer serverOrNull);
}

