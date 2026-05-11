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

import net.minecraft.client.gui.GuiGraphics;
import net.pcal.fastback.common.logging.UserMessage;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Client-only helper services. Only present when running on a client (integrated or dedicated
 * server never provides this). ModImpl holds a nullable reference; null means we are on a
 * dedicated server.
 *
 * @author pcal
 * @since 0.2.0
 */
public interface ClientHelper {

    /** @return path to the 'saves' directory. */
    Path getSavesDir();

    /** @return paths that should be included when mods-backup is enabled. */
    Collection<Path> getModsBackupPaths();

    /** Display ephemeral status text on the HUD. */
    void setHudText(UserMessage userMessage);

    /** Remove text previously set by {@link #setHudText}. */
    void clearHudText();

    /**
     * If a MessageScreen is currently displayed, update its title text.
     * Otherwise does nothing.
     */
    void setMessageScreenText(UserMessage userMessage);

    /**
     * Called by the mixin when a MessageScreen render pass occurs.
     * Used to render the HUD overlay on top of it.
     */
    void renderMessageScreen(GuiGraphics drawContext);
}
