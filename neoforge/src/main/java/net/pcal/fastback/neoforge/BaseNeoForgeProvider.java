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
package net.pcal.fastback.neoforge;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.pcal.fastback.MixinGateway;
import net.pcal.fastback.logging.Log4jLogger;
import net.pcal.fastback.logging.SystemLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mixins.ServerAccessors;
import net.pcal.fastback.mixins.SessionAccessors;
import net.pcal.fastback.mod.LifecycleListener;
import net.pcal.fastback.mod.MinecraftProvider;
import org.apache.logging.log4j.LogManager;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.commands.Commands.createBackupCommand;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.mod.MinecraftProvider.register;
/**
 * Base NeoForge implementation of MinecraftProvider and MixinGateway.
 *
 * @author pcal
 */
abstract class BaseNeoForgeProvider implements MinecraftProvider, MixinGateway {
    static final String MOD_ID = "fastback";
    private MinecraftServer minecraftServer;
    private Runnable autoSaveListener;
    private boolean isWorldSaveEnabled = true;
    protected BaseNeoForgeProvider() {
    }
    @Override
    public void sendBroadcast(UserMessage userMessage) {
        if (this.minecraftServer != null && this.minecraftServer.isDedicatedServer()) {
            minecraftServer.getPlayerList().broadcastSystemMessage(MinecraftProvider.messageToText(userMessage), false);
        }
    }
    @Override
    public String getModVersion() {
        return ModList.get().getModContainerById(MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElseThrow(() -> new IllegalStateException("Could not find mod container for " + MOD_ID));
    }
    @Override
    public void setWorldSaveEnabled(boolean enabled) {
        this.isWorldSaveEnabled = enabled;
    }
    @Override
    public void saveWorld() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        this.minecraftServer.saveEverything(false, true, true);
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
    @Override
    public void addBackupProperties(Map<String, String> props) {
        props.put("fastback-version", this.getModVersion());
        if (this.minecraftServer != null) {
            props.put("minecraft-version", minecraftServer.getServerVersion());
            props.put("minecraft-game-mode", String.valueOf(minecraftServer.getWorldData().getGameType()));
            props.put("minecraft-level-name", minecraftServer.getWorldData().getLevelName());
        }
        try {
            final List<String> modList = new ArrayList<>();
            ModList.get().getMods().forEach(info ->
                    modList.add(info.getModId() + ':' + info.getVersion()));
            Collections.sort(modList);
            final StringBuilder modListProp = new StringBuilder();
            for (final String mod : modList) modListProp.append(mod).append(", ");
            props.put("neoforge-mods", modListProp.toString());
        } catch (Exception ohwell) {
            syslog().error(ohwell);
        }
    }
    @Override
    public Collection<Path> getModsBackupPaths() {
        final Path gameDir = FMLPaths.GAMEDIR.get();
        final List<Path> out = new ArrayList<>();
        out.add(gameDir.resolve("options.txt"));
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
    void onRegisterCommands(RegisterCommandsEvent event) {
        final int requiredLevel = this.isClient() ? 0 : 4;
        LiteralArgumentBuilder<CommandSourceStack> backupCommand = createBackupCommand(
                permName -> source -> source.hasPermission(requiredLevel)
        );
        event.getDispatcher().register(backupCommand);
        syslog().debug("registered backup command");
    }
    LifecycleListener initialize() {
        SystemLogger.Singleton.register(new Log4jLogger(LogManager.getLogger(MOD_ID)));
        final LifecycleListener lifecycle = register(this);
        MixinGateway.Singleton.register(this);
        lifecycle.onInitialize();
        return lifecycle;
    }
}
