package net.pcal.fastback.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.level.storage.LevelStorage;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.fabric.mixins.ScreenAccessors;
import net.pcal.fastback.fabric.mixins.ServerAccessors;
import net.pcal.fastback.fabric.mixins.SessionAccessors;
import net.pcal.fastback.logging.Log4jLogger;
import net.pcal.fastback.logging.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.Files.createTempDirectory;
import static java.util.Objects.requireNonNull;

class FabricModContext implements ModContext {

    private static final String MOD_ID = "fastback";
    private final Logger logger = new Log4jLogger(LogManager.getLogger(MOD_ID));
    private final ExecutorService exs = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final EnvType envType;
    private Path tempRestoresDirectory = null;

    FabricModContext(EnvType envType) {
        this.envType = requireNonNull(envType);
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
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public ExecutorService getExecutorService() {
        return this.exs;
    }

    @Override
    public Path getRestoresDir() throws IOException {
        final MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            return client.getLevelStorage().getSavesDirectory();
        } else {
            if (tempRestoresDirectory == null) {
                tempRestoresDirectory = createTempDirectory(MOD_ID + "-restore");
            }
            return tempRestoresDirectory;
        }
    }

    @Override
    public void setSavingScreenText(Text text) {
        if (envType == EnvType.CLIENT) {
            final MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                final Screen screen = client.currentScreen;
                if (screen instanceof MessageScreen) {
                    ((ScreenAccessors) screen).setTitle(text);
                }
            }
        }
    }

    @Override
    public Path getWorldSaveDirectory(MinecraftServer server) {
        final LevelStorage.Session session = ((ServerAccessors) server).getSession();
        return ((SessionAccessors) session).getDirectory().path();
    }

    @Override
    public String getWorldName(MinecraftServer server) {
        final LevelStorage.Session session = ((ServerAccessors) server).getSession();
        return session.getLevelSummary().getLevelInfo().getLevelName();
    }

    @Override
    public void setWorldSaveEnabled(MinecraftServer mc, boolean enabled) {
        for (ServerWorld serverWorld : mc.getWorlds()) {
            if (serverWorld != null) serverWorld.savingDisabled = !enabled;
        }
    }
}
