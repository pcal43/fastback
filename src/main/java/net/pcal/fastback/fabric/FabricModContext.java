package net.pcal.fastback.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import net.pcal.fastback.ModContext;
import org.apache.logging.log4j.LogManager;

import java.util.Optional;

class FabricModContext implements ModContext {

    private static final String MOD_ID = "fastback";
    private final org.apache.logging.log4j.Logger log4j = LogManager.getLogger("fastback");
    private final Logger logger = new FabricLoggerAdapter(log4j);

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
    public org.apache.logging.log4j.Logger getLog4j() {
        return this.log4j;
    }

}
