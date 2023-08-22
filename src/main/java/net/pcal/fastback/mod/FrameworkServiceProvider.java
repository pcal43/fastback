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

import net.minecraft.text.Text;

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

    /**
     * @return path to the 'saves' directory on a minecraft client, or null if we're on a server.
     */
    Path getSavesDir();

    /**
     * @return path to the directory of the current world.
     */
    Path getWorldDirectory();


    String getWorldName();

    /**
     * @return true if we're clientside.
     */
    boolean isClient();

    /**
     * @return true if world saving is currently enabled (i.e. if we haven't disabled it).
     */
    boolean isWorldSaveEnabled();

    void setWorldSaveEnabled(boolean enabled);

    void saveWorld();

    /**
     * Display ephemeral status text on the screen to the user,.  This could be part of the in-game HUD
     * or any other floating text, depending on what screen the user is on.  Has no effect if we're serverside.
     */
    void setHudText(Text text);

    void setAutoSaveListener(Runnable runnable);
}
