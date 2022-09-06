package net.pcal.fastback.fabric;

import net.pcal.fastback.ModContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;
import net.pcal.fastback.fabric.mixins.ServerAccessors;
import net.pcal.fastback.fabric.mixins.SessionAccessors;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

class FabricWorldContext implements ModContext.WorldContext {

    private final ModContext modContext;
    private final MinecraftServer server;
    private final LevelStorage.Session session;
    private final LevelInfo li;
    private final LevelSummary ls;

    FabricWorldContext(ModContext modContext, MinecraftServer server) {
        this.modContext =requireNonNull(modContext);
        this.server = requireNonNull(server);
        this.session = requireNonNull(((ServerAccessors) server).getSession());
        this.ls = requireNonNull(session.getLevelSummary());
        this.li = requireNonNull(ls.getLevelInfo());
    }

    @Override
    public ModContext getModContext() {
        return this.modContext;
    }

    @Override
    public Path getWorldSaveDirectory() {
        final LevelStorage.Session session = ((ServerAccessors) server).getSession();
        return ((SessionAccessors) session).getDirectory().path();
    }

    @Override
    public String getWorldName() {
        return this.li.getLevelName();
    }

    @Override
    public long getSeed() {
        return server.getSaveProperties().getGeneratorOptions().getSeed();
    }

    @Override
    public String getGameMode() {
        return String.valueOf(li.getGameMode());
    }

    @Override
    public String getDifficulty() {
        return String.valueOf(li.getDifficulty());
    }

    @Override
    public String getMinecraftVersion() {
        // FIXME figure out how to move this to ModContext
        return server.getVersion();
    }

}
