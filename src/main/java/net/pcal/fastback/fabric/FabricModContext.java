package net.pcal.fastback.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import net.pcal.fastback.ModContext;
import org.apache.logging.log4j.LogManager;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class FabricModContext implements ModContext {

    private static final String MOD_ID = "fastback";
    private final Logger logger = new FabricLoggerAdapter(LogManager.getLogger("fastback"));
    private ExecutorService exs = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public String getFastbackModVersion() {
        Optional<ModContainer> optionalModContainer = FabricLoader.getInstance().getModContainer(MOD_ID);
        if (optionalModContainer.isEmpty()) {
            throw new IllegalStateException("Could not find loader for " + MOD_ID);
        }
        return optionalModContainer.get().getMetadata().getVersion().toString();
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
    public WorldContext getWorldContext(MinecraftServer forServer) {
        return new FabricWorldContext(this, forServer);
    }

}
