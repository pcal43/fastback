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

/**
 * Services that must be provided by the underlying mod framework.  Currently, that means fabric only.
 *
 * But abstracting it away like this ensures that it would be relatively straightforward to support other frameworks
 * if there's ever a desire or need for that.
 *
 * @author pcal
 * @since 0.1.0
 */
public interface FrameworkServiceProvider {

    String getModId();

    String getModVersion();

    Path getConfigDir();

    String getMinecraftVersion();

    Path getWorldDirectory();

    String getWorldName();

    Path getSnapshotRestoreDir();

    boolean isClient();

    boolean isWorldSaveEnabled();

    void setWorldSaveEnabled(boolean enabled);

    void saveWorld();

    boolean isServerStopping();

    @Deprecated // just merge with setClientStatusText
    void setClientSavingScreenText(UserMessage message);

    void sendClientChatMessage(UserMessage message);

    /**
     * Display ephemeral status text on the screen to the user, if we have a UI client.  This will either be in the
     * HUD or on the saving screen.
     */
    void setClientOverlayText(UserMessage message);

    void sendFeedback(UserMessage message, ServerCommandSource scs);

    void sendError(UserMessage message, ServerCommandSource scs);

    void setAutoSaveListener(Runnable runnable);
}
