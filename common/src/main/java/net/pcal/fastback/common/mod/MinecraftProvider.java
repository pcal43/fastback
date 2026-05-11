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

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.pcal.fastback.common.MixinGateway;
import net.pcal.fastback.common.logging.Log4jLogger;
import net.pcal.fastback.common.logging.SystemLogger;
import net.pcal.fastback.common.logging.UserMessage;
import net.pcal.fastback.common.mixins.ServerAccessors;
import net.pcal.fastback.common.mixins.SessionAccessors;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static net.minecraft.ChatFormatting.GRAY;
import static net.minecraft.ChatFormatting.GREEN;
import static net.minecraft.ChatFormatting.RED;
import static net.minecraft.ChatFormatting.YELLOW;
import static net.minecraft.network.chat.Style.EMPTY;
import static net.pcal.fastback.common.logging.SystemLogger.syslog;
import static net.pcal.fastback.common.logging.UserMessage.UserMessageStyle.ERROR;

/**
 * Services that must be provided by the underlying mod framework.
 * <p>
 * Loader-agnostic implementations live here. Each loader subclass overrides only what
 * differs: {@link #getModVersion()}, {@link #getModsBackupPaths()}, and
 * {@link #addBackupProperties(Map)} (for the loader-specific mod list), plus command
 * registration inside {@link #initialize()}.
 *
 * @author pcal
 * @since 0.1.0
 */
public abstract class MinecraftProvider implements MixinGateway {

    // ======================================================================
    // Constants

    public static final String MOD_ID = "fastback";

    // ======================================================================
    // Fields

    private MinecraftServer minecraftServer;
    private Runnable autoSaveListener;
    private boolean isWorldSaveEnabled = true;

    // ======================================================================
    // Static helpers

    public static LifecycleListener register(final MinecraftProvider sp) {
        final ModImpl mod = new ModImpl(sp);
        Mod.Singleton.register(mod);
        return mod;
    }

    public static Component messageToText(final UserMessage m) {
        final MutableComponent out;
        if (m.localized() != null) {
            out = Component.translatable(m.localized().key(), m.localized().params());
        } else {
            out = Component.literal(m.raw());
        }
        switch (m.style()) {
            case ERROR -> out.setStyle(EMPTY.withColor(TextColor.fromLegacyFormat(RED)));
            case WARNING -> out.setStyle(EMPTY.withColor(TextColor.fromLegacyFormat(YELLOW)));
            case JGIT -> out.setStyle(EMPTY.withColor(TextColor.fromLegacyFormat(GRAY)));
            case NATIVE_GIT -> out.setStyle(EMPTY.withColor(TextColor.fromLegacyFormat(GREEN)));
        }
        return out;
    }

    // ======================================================================
    // Abstract methods — must be implemented by each loader subclass

    /** @return the version of the fastback mod. */
    public abstract String getModVersion();

    /** @return path to the 'saves' directory on a minecraft client, or null if on a server. */
    public abstract Path getSavesDir();

    /** @return true if we're clientside. */
    public abstract boolean isClient();

    /** Display ephemeral status text on screen. Has no effect serverside. */
    public abstract void setHudText(UserMessage userMessage);

    /** Remove text set by setHudText. */
    public abstract void clearHudText();

    /**
     * If a minecraft MessageScreen is being displayed, set its title.
     * Otherwise does nothing.
     */
    public abstract void setMessageScreenText(UserMessage userMessage);

    /** @return paths to backup when mods-backup is enabled. */
    public abstract Collection<Path> getModsBackupPaths();

    // ======================================================================
    // Shared implementations

    public void sendBroadcast(UserMessage userMessage) {
        if (this.minecraftServer != null && this.minecraftServer.isDedicatedServer()) {
            minecraftServer.getPlayerList().broadcastSystemMessage(messageToText(userMessage), false);
        }
    }

    public void setWorldSaveEnabled(boolean enabled) {
        this.isWorldSaveEnabled = enabled;
    }

    public void saveWorld() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        this.minecraftServer.saveEverything(false, true, true);
    }

    public void setAutoSaveListener(Runnable runnable) {
        if (this.autoSaveListener != null) throw new IllegalStateException();
        this.autoSaveListener = requireNonNull(runnable);
    }

    public Path getWorldDirectory() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        final LevelStorageSource.LevelStorageAccess session =
                ((ServerAccessors) this.minecraftServer).getStorageSource();
        return ((SessionAccessors) session).getLevelDirectory().path();
    }

    public String getWorldName() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        return this.minecraftServer.getWorldData().getLevelName();
    }

    /**
     * Adds the minecraft-* and fastback-version properties shared by all loaders.
     * Subclasses should call {@code super.addBackupProperties(props)} and then append
     * their own loader-specific mod list.
     */
    public void addBackupProperties(Map<String, String> props) {
        props.put("fastback-version", this.getModVersion());
        if (this.minecraftServer != null) {
            props.put("minecraft-version", minecraftServer.getServerVersion());
            props.put("minecraft-game-mode", String.valueOf(minecraftServer.getWorldData().getGameType()));
            props.put("minecraft-level-name", minecraftServer.getWorldData().getLevelName());
        }
    }

    public void sendChat(UserMessage message, CommandSourceStack scs) {
        if (message.style() == ERROR) {
            scs.sendFailure(messageToText(message));
        } else {
            scs.sendSuccess(() -> messageToText(message), false);
        }
    }

    // ======================================================================
    // MixinGateway shared implementations

    @Override
    public boolean isWorldSaveEnabled() {
        return this.isWorldSaveEnabled;
    }

    @Override
    public void autoSaveCompleted() {
        if (this.autoSaveListener != null) {
            this.autoSaveListener.run();
        } else {
            syslog().warn("Autosave just happened but, unexpectedly, no one is listening.");
        }
    }

    // ======================================================================
    // Shared initialization

    /**
     * Performs loader-agnostic initialization: registers the system logger, the
     * {@link MinecraftProvider} singleton, and the {@link MixinGateway} singleton,
     * then fires {@link LifecycleListener#onInitialize()}.
     * <p>
     * Subclasses should call {@code super.initialize()} and then add loader-specific
     * setup (e.g. command registration).
     */
    protected LifecycleListener initialize() {
        SystemLogger.Singleton.register(new Log4jLogger(LogManager.getLogger(MOD_ID)));
        final LifecycleListener lifecycle = register(this);
        MixinGateway.Singleton.register(this);
        lifecycle.onInitialize();
        return lifecycle;
    }

    // ======================================================================
    // Lifecycle helpers for subclasses

    public void setMinecraftServer(MinecraftServer serverOrNull) {
        if ((serverOrNull == null) == (this.minecraftServer == null)) throw new IllegalStateException();
        this.minecraftServer = serverOrNull;
    }

    protected MinecraftServer getMinecraftServer() {
        return this.minecraftServer;
    }
}
