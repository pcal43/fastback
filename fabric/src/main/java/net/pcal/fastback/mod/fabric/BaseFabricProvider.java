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

package net.pcal.fastback.mod.fabric;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.pcal.fastback.logging.Log4jLogger;
import net.pcal.fastback.logging.SystemLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mod.LifecycleListener;
import net.pcal.fastback.mod.MinecraftProvider;
import net.pcal.fastback.mod.fabric.mixins.ServerAccessors;
import net.pcal.fastback.mod.fabric.mixins.SessionAccessors;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.commands.Commands.createBackupCommand;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.mod.MinecraftProvider.messageToText;
import static net.pcal.fastback.mod.MinecraftProvider.register;

/**
 * @author pcal
 * @since 0.1.0
 */
abstract class BaseFabricProvider implements MinecraftProvider, MixinGateway {

    static final String MOD_ID = "fastback";

    private MinecraftServer minecraftServer;
    private Runnable autoSaveListener;

    private boolean isWorldSaveEnabled = true;

    protected BaseFabricProvider() {
    }

    @Override
    public void sendBroadcast(UserMessage userMessage) {
        if (this.minecraftServer != null && this.minecraftServer.isDedicatedServer()) {
            minecraftServer.getPlayerList().broadcastSystemMessage(messageToText(userMessage), false);
        }
    }

    @Override
    public String getModVersion() {
        Optional<ModContainer> optionalModContainer = FabricLoader.getInstance().getModContainer(MOD_ID);
        if (optionalModContainer.isEmpty()) {
            throw new IllegalStateException("Could not find loader for " + MOD_ID);
        }
        final ModMetadata m = optionalModContainer.get().getMetadata();
        return String.valueOf(m.getVersion());
    }

    @Override
    public void setWorldSaveEnabled(boolean enabled) {
        this.isWorldSaveEnabled = enabled;
    }

    @Override
    public void saveWorld() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        this.minecraftServer.saveEverything(false, true, true); // suppressLogs, flush, force
    }

    @Override
    public void setAutoSaveListener(Runnable runnable) {
        if (this.autoSaveListener != null) throw new IllegalStateException();
        this.autoSaveListener = requireNonNull(runnable);
    }

    @Override
    public Path getWorldDirectory() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        final LevelStorageSource.LevelStorageAccess session = ((ServerAccessors) this.minecraftServer).getStorageSource();
        return ((SessionAccessors) session).getLevelDirectory().path();
    }

    @Override
    public String getWorldName() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        return this.minecraftServer.getWorldData().getLevelName();
    }

    /**
     * Add extra properties that will be stored in .fastback/backup.properties.
     */
    public void addBackupProperties(Map<String, String> props) {
        props.put("fastback-version", this.getModVersion());
        if (this.minecraftServer != null) {
            props.put("minecraft-version", minecraftServer.getServerVersion());
            props.put("minecraft-game-mode", String.valueOf(minecraftServer.getWorldData().getGameType()));
            props.put("minecraft-level-name", minecraftServer.getWorldData().getLevelName());
        }
        try {
            final Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
            final List<String> modList = new ArrayList<>();
            for (final ModContainer mc : mods) {
                modList.add(mc.getMetadata().getId() + ':' + mc.getMetadata().getVersion());
            }
            Collections.sort(modList);
            final StringBuilder modListProp = new StringBuilder();
            for (final String mod : modList) modListProp.append(mod + ", ");
            props.put("fabric-mods", modListProp.toString());
        } catch (Exception ohwell) {
            syslog().error(ohwell);
        }
    }

    /**
     * @return paths to the files and directories that should be backed up when config-backup is enabled.
     */
    @Override
    public Collection<Path> getModsBackupPaths() {
        final List<Path> out = new ArrayList<>();
        final FabricLoader fl = FabricLoader.getInstance();
        final Path gameDir = fl.getGameDir();
        out.add(gameDir.resolve("options.txt´"));
        out.add(gameDir.resolve("mods"));
        out.add(gameDir.resolve("config"));
        out.add(gameDir.resolve("resourcepacks"));
        return out;
    }

    // ======================================================================
    // MixinGateway implementation

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
    // Package private

    void setMinecraftServer(MinecraftServer serverOrNull) {
        if ((serverOrNull == null) == (this.minecraftServer == null)) throw new IllegalStateException();
        this.minecraftServer = serverOrNull;
    }

    /**
     * This is the key initialization routine.  Registers the logger, the frameworkprovider and the commands
     * where the rest of the mod can get at them.
     */
    LifecycleListener initialize() {
        SystemLogger.Singleton.register(new Log4jLogger(LogManager.getLogger(MOD_ID)));
        final LifecycleListener lifecycle = register(this);
        LiteralArgumentBuilder<CommandSourceStack> backupCommand = createBackupCommand(permName -> {
            final int requiredLevel = this.isClient() ? 0 : 4;
            return Permissions.require(permName, requiredLevel);
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, regAccess, env) -> dispatcher.register(backupCommand));
        syslog().debug("registered backup command");
        MixinGateway.Singleton.register(this);
        lifecycle.onInitialize();
        return lifecycle;
    }


}
