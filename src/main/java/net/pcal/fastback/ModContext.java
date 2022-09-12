package net.pcal.fastback;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.pcal.fastback.logging.Log4jLogger;
import net.pcal.fastback.logging.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.file.Files.createTempDirectory;
import static java.util.Objects.requireNonNull;

public class ModContext {

    private final ModFrameworkProvider spi;
    private final Logger log;
    private final ExecutorService exs;
    private Path tempRestoresDirectory = null;

    public static ModContext create(ModFrameworkProvider spi) {
        final Logger logger = new Log4jLogger(LogManager.getLogger(spi.getModId()));
        final ExecutorService exs = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        return new ModContext(spi, exs, logger);
    }

    private ModContext(ModFrameworkProvider spi, ExecutorService exs, Logger log) {
        this.spi = requireNonNull(spi);
        this.exs = requireNonNull(exs);
        this.log = requireNonNull(log);
    }

    public ExecutorService getExecutorService() {
        return this.exs;
    }

    public String getCommandName() {
        return "backup";
    }

    public boolean isUnsafeCommandsEnabled() {
        return false;
    }

    public boolean isStartupNotificationEnabled() {
        return true;
    }

    public Path getRestoresDir() throws IOException {
        Path restoreDir = this.spi.getClientSavesDir();
        if (restoreDir != null) return restoreDir;
        if (tempRestoresDirectory == null) {
            tempRestoresDirectory = createTempDirectory(getModId() + "-restore");
        }
        return tempRestoresDirectory;
    }

    // PASSTHROUGH IMPLEMENTATIONS

    String getModId() {
        return this.spi.getModId();
    }

    public String getModVersion() {
        return this.spi.getModVersion();
    }

    public String getMinecraftVersion() {
        return this.spi.getMinecraftVersion();
    }

    public void setWorldSaveEnabled(MinecraftServer mc, boolean enabled) {
        this.spi.setWorldSaveEnabled(mc, enabled);
    }

    public boolean isClient() {
        return spi.isClient();
    }

    public void setSavingScreenText(Text text) {
        this.spi.setClientSavingScreenText(text);
    }

    public void sendClientChatMessage(Text text) {
        this.spi.sendClientChatMessage(text);
    }

    public Path getWorldSaveDirectory(MinecraftServer server) {
        return this.spi.getWorldDirectory(server);
    }

    public String getWorldName(MinecraftServer server) {
        return this.spi.getWorldName(server);
    }

    public Logger getLogger() {
        return this.spi.getLogger();
    }

    public interface ModFrameworkProvider {

        Logger getLogger();

        String getModId();

        String getModVersion();

        String getMinecraftVersion();

        Path getWorldDirectory(MinecraftServer server);

        String getWorldName(MinecraftServer server);

        void setWorldSaveEnabled(MinecraftServer mc, boolean enabled);

        void setClientSavingScreenText(Text text);

        void sendClientChatMessage(Text text);

        Path getClientSavesDir() throws IOException;

        boolean isClient();
    }
}
