package net.pcal.fastback.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

class FabricFrameworkProvider implements ModContext.ModFrameworkProvider {

    private static final String MOD_ID = "fastback";
    private FabricClientProvider clientProvider;
    final Logger logger = new Log4jLogger(LogManager.getLogger(MOD_ID));

    void setClientProvider(FabricClientProvider clientSpi) {
        if (this.clientProvider != null) throw new IllegalStateException();
        this.clientProvider = requireNonNull(clientSpi);
    }

    @Override
    public Logger getLogger(){
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
    public Path getWorldDirectory(final MinecraftServer server) {
        final LevelStorage.Session session = ((ServerAccessors) server).getSession();
        return ((SessionAccessors) session).getDirectory().path();
    }

    @Override
    public String getWorldName(final MinecraftServer server) {
        final LevelStorage.Session session = ((ServerAccessors) server).getSession();
        return session.getLevelSummary().getLevelInfo().getLevelName();
    }

    @Override
    public void setWorldSaveEnabled(final MinecraftServer mc, final boolean enabled) {
        for (ServerWorld serverWorld : mc.getWorlds()) {
            if (serverWorld != null) serverWorld.savingDisabled = !enabled;
        }
    }
}
