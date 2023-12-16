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
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.pcal.fastback.logging.UserMessage;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import static net.minecraft.text.Style.EMPTY;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;

/**
 * Services that must be provided by the underlying mod framework.  Currently, that means fabric only.
 * <p>
 * But abstracting it away like this ensures that it would be relatively straightforward to support other
 * frameworks if there's ever a desire or need for that.  If you're interested in helping port Fastback
 * to (say) Forge, this is the place to start.
 *
 * @author pcal
 * @since 0.1.0
 */
public interface MinecraftProvider {

    static LifecycleListener register(final MinecraftProvider sp) {
        final ModImpl mod = new ModImpl(sp);
        Mod.Singleton.register(mod);
        return mod;
    }

    /**
     * @return the version of the fastback mod.
     */
    String getModVersion();

    /**
     * @return path to the 'saves' directory on a minecraft client, or null if we're on a server.
     */
    Path getSavesDir();

    /**
     * @return path to the directory of the current world, or null if no world is loaded.
     */
    Path getWorldDirectory();


    /**
     * @return the name of the current world, or null if no world is loaded.
     */
    String getWorldName();

    /**
     * @return true if we're clientside.
     */
    boolean isClient();

    /**
     * If on a server, broadcasts a message to all connected users.
     */
    void sendBroadcast(UserMessage userMessage);

    /**
     * Enable or disable world saving.
     */
    void setWorldSaveEnabled(boolean enabled);

    /**
     * Force a world save to start now.  Called when a manual backup is performed.
     */
    void saveWorld();

    /**
     * Display ephemeral status text on the screen to the user,.  This could be part of the in-game HUD
     * or any other floating text, depending on what screen the user is on.  Has no effect if we're serverside.
     */
    void setHudText(UserMessage userMessage);

    /**
     * Remove text set by setHudText.
     */
    void clearHudText();

    /**
     * If we're clientside and a minecraft MessageScreen is being displayed (e.g., the 'saving' screen), set
     * the title of the screen.  Otherwise does nothing.
     */
    void setMessageScreenText(UserMessage userMessage);

    /**
     * Register a callback that should be called after an autosave completes.
     */
    void setAutoSaveListener(Runnable runnable);

    /**
     * Add some interesting properties to record in backup.properties.
     */
    void addBackupProperties(Map<String, String> props);

    /**
     * @return paths to backup when mods-backup enabled.
     */
    Collection<Path> getModsBackupPaths();

    /**
     * Send a chat message to user.
     */
    default void sendChat(UserMessage message, ServerCommandSource scs) {
        if (message.style() == ERROR) {
            scs.sendError(messageToText(message));
        } else {
            scs.sendFeedback(() -> messageToText(message), false);
        }
    }

    /**
     * Utility class that implementing classes can use to perform a standard conversion of UserMessage to minecraft Text.
     */
    static Text messageToText(final UserMessage m) {
        final MutableText out;
        if (m.localized() != null) {
            out = Text.translatable(m.localized().key(), m.localized().params());
        } else {
            out = Text.literal(m.raw());
        }
        switch (m.style()) {
            case ERROR -> {
                out.setStyle(EMPTY.withColor(TextColor.fromFormatting(Formatting.RED)));
            }
            case WARNING -> {
                out.setStyle(EMPTY.withColor(TextColor.fromFormatting(Formatting.YELLOW)));
            }
            case JGIT -> {
                out.setStyle(EMPTY.withColor(TextColor.fromFormatting(Formatting.GRAY)));
            }
            case NATIVE_GIT -> {
                out.setStyle(EMPTY.withColor(TextColor.fromFormatting(Formatting.GREEN)));
            }
            case BROADCAST -> {
                out.setStyle(EMPTY.withItalic(true));
            }
            default -> {
                out.setStyle(EMPTY.withColor(TextColor.fromFormatting(Formatting.WHITE)));
            }
        }
        return out;
    }

}
