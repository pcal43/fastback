package net.pcal.gitback;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

public interface ModContext {

    String getModId();

    String getModVersion();

    String getMinecraftVersion();

    Loggr getLogger();

    ExecutorService getExecutorService();

    Path getClientSavesDir();

    Path getWorldSaveDirectory(MinecraftServer server);

    String getWorldName(MinecraftServer server);

    void enableWorldSaving(MinecraftServer mc, boolean enabled);

    default String getCommandName() {
        return "backup";
    }

}
