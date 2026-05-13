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

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.pcal.fastback.common.logging.UserMessage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Singleton that provides various mod-wide services.
 *
 * @author pcal
 * @since 0.1.0
 */
public interface Mod {

    // ======================================================================
    // Singleton

    static Mod mod() {
        return SingletonHolder.INSTANCE;
    }

    class SingletonHolder {
        private static Mod INSTANCE = null;

        public static void register(Mod mod) {
            requireNonNull(mod);
            if (INSTANCE != null) throw new IllegalStateException("Mod singleton initialized twice");
            SingletonHolder.INSTANCE = mod;
        }
    }

    // ======================================================================
    // Loader-facing methods

    /**
     * Initializes the mod for a dedicated server. Call once at startup.
     */
    static void initializeForDedicatedServer(LoaderHelper loaderHelper) {
        ModImpl.initialize(loaderHelper, null);
    }

    /**
     * Initializes the mod for a client (integrated or dedicated-server-from-client). Call once at startup.
     */
    static void initializeForClient(LoaderHelper loaderHelper, ClientHelper clientHelper) {
        ModImpl.initialize(loaderHelper, clientHelper);
    }

    /**
     * Must be called when a world is starting so that we can have a reference
     * to the active world.
     */
    void onWorldStart(MinecraftServer server);

    /**
     * Must be called when a world is stopping to ensure we can run a
     * shutdown backup.
     */
    void onWorldStop();

    /**
     * Allows loaders to plugin HUD rendering.
     */
    void renderHud(GuiGraphicsExtractor drawContext);

    // ======================================================================
    // Mixin-facing methods

    /**
     * Called from the mixins to check whether vanilla autosaving should
     * be disabled.  Autosaving while a backup is in progress could result
     * in an inconsistent backup state.
     */
    boolean isWorldSaveEnabled();

    /**
     * Called from the mixins to tell us that an autosave just finished.
     * This may trigger a backup, depending on configuration.
     */
    void autoSaveCompleted();

    /**
     * Called from the shutdown message screen mixins to render additional text.
     */
    void renderMessageScreen(GuiGraphicsExtractor drawContext);


    // ======================================================================
    // Command-facing methods

    /**
     * @return path to where snapshots should be restored.
     */
    Path getDefaultRestoresDir() throws IOException;

    /**
     * @return the version of the fastback mod.
     */
    String getModVersion();

    /**
     * Enables or disables world saving.
     */
    void setWorldSaveEnabled(boolean enabled);

    /**
     * Forces a save of the world.  We often want to do this before doing a backup.
     */
    void saveWorld();

    /**
     * If we're clientside and the user is looking at a MessageScreen, set the title.
     */
    void setMessageScreenText(UserMessage message);

    /**
     * Send a chat message to user.
     */
    void sendChat(UserMessage message, CommandSourceStack scs);

    /**
     * If on a dedicated server, broadcast a message to the chat window of all connected users.
     */
    void sendBroadcast(UserMessage message);

    /**
     * Set magical floating text.  You MUST call clearHudText
     */
    void setHudText(UserMessage message);

    /**
     * Remove the magical floating text.
     */
    void clearHudText();

    /**
     * @return path to the save directory of the currently-loaded world (aka the git worktree).
     */
    Path getWorldDirectory();

    /**
     * @return name of the currently-loaded world.
     */
    String getWorldName();

    /**
     * @return paths to backup when mods-backup enabled.
     */
    Collection<Path> getModsBackupPaths();

    /**
     * Add extra properties that will be stored in .fastback/backup.properties.
     */
    void addBackupProperties(Map<String, String> props);
}
