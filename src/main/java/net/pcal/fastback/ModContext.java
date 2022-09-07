package net.pcal.fastback;

import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

public interface ModContext {

    String getFastbackModVersion();

    Loggr getLogger();

    ExecutorService getExecutorService();

    WorldContext getWorldContext(MinecraftServer forServer);

    Path getWorldSaveDirectory(MinecraftServer server);

//    @Deprecated
//    Logger getLog4j(); //KILLME

    interface WorldContext {

        String getWorldUuid() throws IOException;

        Path getWorldSaveDirectory();

        String getWorldName();

        long getSeed();

        String getGameMode();

        String getDifficulty();

        String getMinecraftVersion();

        ModContext getModContext();

    }

}
