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

package net.pcal.fastback.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.level.storage.LevelStorage;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.fabric.mixins.ServerAccessors;
import net.pcal.fastback.fabric.mixins.SessionAccessors;
import net.pcal.fastback.logging.Log4jLogger;
import net.pcal.fastback.logging.Logger;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Path;
import java.util.Optional;

public class FabricFrameworkProvider implements ModContext.ModFrameworkProvider {

    private static FabricFrameworkProvider INSTANCE;


    public static FabricFrameworkProvider getInstance() {
        if (INSTANCE == null) throw new IllegalStateException("not initialized");
        return INSTANCE;
    }

    private static final String MOD_ID = "fastback";
    private boolean isWorldSaveEnabled = true;
    private final FabricClientProvider clientProvider;
    private final Logger logger = new Log4jLogger(LogManager.getLogger(MOD_ID));

    static FabricFrameworkProvider forServer() {
        if (INSTANCE != null) throw new IllegalStateException();
        return INSTANCE = new FabricFrameworkProvider(null);
    }

    static FabricFrameworkProvider forClient(FabricClientProvider clientProvider) {
        if (INSTANCE != null) throw new IllegalStateException();
        return INSTANCE = new FabricFrameworkProvider(clientProvider);
    }

    private FabricFrameworkProvider(FabricClientProvider clientProviderOrNull) {
        this.clientProvider = clientProviderOrNull;
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public String getMinecraftVersion() {
        return SharedConstants.getGameVersion().getName();
    }

    @Override
    public String getModId() {
        return MOD_ID;
    }

    @Override
    public String getModVersion() {
        Optional<ModContainer> optionalModContainer = FabricLoader.getInstance().getModContainer(MOD_ID);
        if (optionalModContainer.isEmpty()) {
            throw new IllegalStateException("Could not find loader for " + MOD_ID);
        }
        final ModMetadata m = optionalModContainer.get().getMetadata();
        return m.getName() + " " + m.getVersion();
    }

    @Override
    public boolean isClient() {
        return this.clientProvider != null;
    }

    @Override
    public boolean isWorldSaveEnabled() {
        return this.isWorldSaveEnabled;
    }

    @Override
    public void setWorldSaveEnabled(boolean enabled) {

    }

    @Override
    public Path getClientSavesDir() {
        if (this.clientProvider != null) {
            Path restoreDir = clientProvider.getClientRestoreDir();
            if (restoreDir != null) return restoreDir;
            this.logger.warn("getClientRestoreDir unexpectedly null, using temp dir");
        }
        return null;
    }

    @Override
    public void setClientSavingScreenText(final Text text) {
        if (this.clientProvider != null) {
            this.clientProvider.consumeSaveScreenText(text);
        }
    }

    @Override
    public void sendClientChatMessage(final Text text) {
        if (this.clientProvider != null) {
            this.clientProvider.sendClientChatMessage(text);
        }
    }

    @Override
    public Path getWorldDirectory(final MinecraftServer server) {
        final LevelStorage.Session session = ((ServerAccessors) server).getSession();
        return ((SessionAccessors) session).getDirectory().path();
    }

    @Override
    public String getWorldName(final MinecraftServer server) {
        final LevelStorage.Session session = ((ServerAccessors) server).getSession();
        return session.getLevelSummary().getLevelInfo().getLevelName();
    }
}
