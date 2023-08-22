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

package net.pcal.fastback.mod;

import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.logging.UserMessage;

import java.nio.file.Path;

public interface FrameworkServiceProvider {

    String getModId();

    String getModVersion();

    Path getConfigDir();

    String getMinecraftVersion();

    Path getWorldDirectory();

    String getWorldName();

    void setClientSavingScreenText(UserMessage message);

    void sendClientChatMessage(UserMessage message);

    Path getSnapshotRestoreDir();

    boolean isClient();

    boolean isWorldSaveEnabled();

    void setWorldSaveEnabled(boolean enabled);

    void saveWorld();

    boolean isServerStopping();

    void setHudText(UserMessage message);

    void sendFeedback(UserMessage message, ServerCommandSource scs);

    void sendError(UserMessage message, ServerCommandSource scs);

    void setAutoSaveListener(Runnable runnable);
}
