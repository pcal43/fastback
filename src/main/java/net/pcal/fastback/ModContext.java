package net.pcal.fastback;

import net.minecraft.server.MinecraftServer;
import org.eclipse.jgit.api.errors.RefNotFoundException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

public interface ModContext {

    String getFastbackModVersion();

    Logger getLogger();

    ExecutorService getExecutorService();

    WorldContext getWorldContext(MinecraftServer forServer);

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

    interface Logger {

        void error(String message);

        void error(String message, Throwable t);

        void warn(String message);

        void warn(String s, IOException ioe);

        void info(String message);

        void debug(String message);
        void debug(Supplier<?> messageSupplier);

        void trace(String message);

        void trace(Supplier<?> messageSupplier);

        void trace(Supplier<?> s, Throwable t);
    }

}
