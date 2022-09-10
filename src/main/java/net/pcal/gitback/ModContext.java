package net.pcal.gitback;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.pcal.gitback.logging.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

public interface ModContext {

    String getModId();

    String getModVersion();

    String getMinecraftVersion();

    Logger getLogger();

    ExecutorService getExecutorService();

    Path getRestoresDir() throws IOException;

    void setSavingScreenText(Text text);

    Path getWorldSaveDirectory(MinecraftServer server);

    String getWorldName(MinecraftServer server);

    void setWorldSaveEnabled(MinecraftServer mc, boolean enabled);

    default String getCommandName() {
        return "backup";
    }

}
