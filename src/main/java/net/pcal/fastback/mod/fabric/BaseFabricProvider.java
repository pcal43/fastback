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

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.world.level.storage.LevelStorage;
import net.pcal.fastback.mod.FrameworkServiceProvider;
import net.pcal.fastback.mod.fabric.mixins.ServerAccessors;
import net.pcal.fastback.mod.fabric.mixins.SessionAccessors;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.SystemLogger.syslog;

/**
 * @author pcal
 * @since 0.1.0
 */
public abstract class BaseFabricProvider implements FrameworkServiceProvider, MixinGateway {

    static final String MOD_ID = "fastback";

    private static BaseFabricProvider INSTANCE;
    private MinecraftServer minecraftServer;
    private Runnable autoSaveListener;

    public static BaseFabricProvider getInstance() {
        if (INSTANCE == null) throw new IllegalStateException("not initialized");
        return INSTANCE;
    }

    private boolean isWorldSaveEnabled = true;

    protected BaseFabricProvider() {
        if (INSTANCE != null) throw new IllegalStateException();
        INSTANCE = this;
    }

    void setMinecraftServer(MinecraftServer serverOrNull) {
        if ((serverOrNull == null) == (this.minecraftServer == null)) throw new IllegalStateException();
        this.minecraftServer = serverOrNull;
    }

    @Override
    public void sendBroadcast(Text text) {
        if (this.minecraftServer != null) {
            minecraftServer.getPlayerManager().broadcast(text, false);
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
        this.minecraftServer.saveAll(false, true, true); // suppressLogs, flush, force
    }

    @Override
    public void setAutoSaveListener(Runnable runnable) {
        if (this.autoSaveListener != null) throw new IllegalStateException();
        this.autoSaveListener = requireNonNull(runnable);
    }

    @Override
    public Path getWorldDirectory() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        final LevelStorage.Session session = ((ServerAccessors) this.minecraftServer).getSession();
        return ((SessionAccessors) session).getDirectory().path();
    }

    @Override
    public String getWorldName() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        final LevelStorage.Session session = ((ServerAccessors) this.minecraftServer).getSession();
        return session.getLevelSummary().getLevelInfo().getLevelName();
    }

    @Override
    public boolean isDedicatedServer() {
        return this.minecraftServer != null && this.minecraftServer.isDedicated();
    }

    /**
     * Add extra properties that will be stored in .fastback/backup.properties.
     */
    public void addBackupProperties(Map<String, String> props) {
        props.put("fastback-version", this.getModVersion());
        if (this.minecraftServer != null) {
            props.put("minecraft-version", minecraftServer.getVersion());
            props.put("minecraft-game-mode", String.valueOf(minecraftServer.getSaveProperties().getGameMode()));
            props.put("minecraft-level-name", minecraftServer.getSaveProperties().getLevelName());
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

}
