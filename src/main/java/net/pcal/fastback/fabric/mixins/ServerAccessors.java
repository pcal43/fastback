package net.pcal.fastback.fabric.mixins;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface ServerAccessors {

    @Accessor
    LevelStorage.Session getSession();
}
