package net.pcal.fastback;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorage;
import net.pcal.fastback.mixins.ServerAccessors;
import net.pcal.fastback.mixins.SessionAccessors;

import java.nio.file.Path;

public class MinecraftUtils {

    public static Path getWorldSaveDir(MinecraftServer server) {
        final LevelStorage.Session session = ((ServerAccessors) server).getSession();
        return ((SessionAccessors) session).getDirectory().path();
    }

}
