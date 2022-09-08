package net.pcal.fastback;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

public interface ModContext {

    String getMinecraftVersion();

    String getFastbackModVersion();

    Loggr getLogger();

    ExecutorService getExecutorService();

    Path getClientSavesDir();

    Path getWorldSaveDirectory(MinecraftServer server);

    String getWorldName(MinecraftServer server);

    void enableWorldSaving(MinecraftServer mc, boolean enabled);
}
