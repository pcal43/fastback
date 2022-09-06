package net.pcal.fastback;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.function.Supplier;

public interface ModContext {

    String getFastbackModVersion();

    Logger getLogger();

    WorldContext getWorldContext(MinecraftServer forServer);

    @Deprecated
    org.apache.logging.log4j.Logger getLog4j(); //KILLME

    interface WorldContext {

        ModContext getModContext();

        Path getWorldSaveDirectory();

        String getWorldName();

        long getSeed();

        String getGameMode();

        String getDifficulty();

        String getMinecraftVersion();
    }

    interface Logger {

        void error(String message);

        void error(String message, Throwable t);

        void warn(String message);

        void info(String message);

        void debug(String message);

        void trace(String message);

        void trace(Supplier<?> messageSupplier);

    }

}
