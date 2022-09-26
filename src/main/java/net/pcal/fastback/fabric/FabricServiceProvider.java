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
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.world.level.storage.LevelStorage;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.fabric.mixins.ServerAccessors;
import net.pcal.fastback.fabric.mixins.SessionAccessors;
import net.pcal.fastback.logging.Log4jLogger;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.Message;
import org.apache.logging.log4j.LogManager;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

public class FabricServiceProvider implements ModContext.FrameworkServiceProvider {

    private static FabricServiceProvider INSTANCE;
    private MinecraftServer minecraftServer;

    public static FabricServiceProvider getInstance() {
        if (INSTANCE == null) throw new IllegalStateException("not initialized");
        return INSTANCE;
    }

    private static final String MOD_ID = "fastback";
    private boolean isWorldSaveEnabled = true;
    private final FabricClientProvider clientProvider;
    private final Function<Message, String> localizer = String::valueOf; //FIXME we don't know how to do this
    private final Logger logger = new Log4jLogger(LogManager.getLogger(MOD_ID), localizer);

    static FabricServiceProvider forDedicatedServer() {
        if (INSTANCE != null) throw new IllegalStateException();
        return INSTANCE = new FabricServiceProvider(null);
    }

    static FabricServiceProvider forClient(FabricClientProvider clientProvider) {
        if (INSTANCE != null) throw new IllegalStateException();
        return INSTANCE = new FabricServiceProvider(clientProvider);
    }

    private FabricServiceProvider(FabricClientProvider clientProviderOrNull) {
        this.clientProvider = clientProviderOrNull;
    }

    void setMinecraftServer(MinecraftServer serverOrNull) {
        if ((serverOrNull == null) == (this.minecraftServer == null)) throw new IllegalStateException();
        this.minecraftServer = serverOrNull;
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
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
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
        return String.valueOf(m.getVersion());
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
        this.isWorldSaveEnabled = enabled;
    }

    @Override
    public void saveWorld() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        this.minecraftServer.saveAll(false, true, true); // suppressLogs, flush, force
    }

    @Override
    public boolean isServerStopping() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        return this.minecraftServer.isStopped() || this.minecraftServer.isStopping();
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
    public void setClientSavingScreenText(Message message) {
        if (this.clientProvider != null) {
            this.clientProvider.consumeSaveScreenText(messageToText(message));
        }
    }

    @Override
    public void sendClientChatMessage(Message message) {
        if (this.clientProvider != null) {
            this.clientProvider.sendClientChatMessage(messageToText(message));
        }
    }

    @Override
    public void sendFeedback(Message message, ServerCommandSource scs) {
        scs.sendFeedback(messageToText(message), false);
    }

    @Override
    public void sendError(Message message, ServerCommandSource scs) {
        scs.sendError(messageToText(message));
    }

    @Override
    public Path getWorldDirectory() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        final LevelStorage.Session session = ((ServerAccessors) this.minecraftServer).getSession();
        return ((SessionAccessors) session).getDirectory().path();
    }

    @Override
    public Path getWorldDirectory(final MinecraftServer server) {
        final LevelStorage.Session session = ((ServerAccessors) server).getSession();
        return ((SessionAccessors) session).getDirectory().path();
    }

    @Override
    public String getWorldName() {
        if (this.minecraftServer == null) throw new IllegalStateException();
        final LevelStorage.Session session = ((ServerAccessors) this.minecraftServer).getSession();
        return session.getLevelSummary().getLevelInfo().getLevelName();
    }

    @Override
    public String getWorldName(final MinecraftServer server) {
        final LevelStorage.Session session = ((ServerAccessors) server).getSession();
        return session.getLevelSummary().getLevelInfo().getLevelName();
    }

    private static Text messageToText(final Message m) {
        if (m.localized() != null) {
            return Text.translatable(m.localized().key(), m.localized().params());
        } else {
            return Text.literal(m.raw());
        }
    }


    /**
    private static String getString(Text message) {
        if (message.getContent() instanceof TranslatableTextContent) {
            // FIXME this doesn't work - Language.getInstance() doesn't have the mod keys.
            // FIXME Figure out how to translate it ourselves properly
            final String key = ((TranslatableTextContent) message.getContent()).getKey();
            if (Language.getInstance().hasTranslation(key)) return Language.getInstance().get(key);
        }
        return message.getString();
    }
    **/

}
