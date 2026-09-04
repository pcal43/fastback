package net.pcal.fastback.common.mixins;

import net.minecraft.util.DirectoryLock;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelStorageSource.class)
public interface LevelStorageSourceAccessor {
    @Accessor(value = "LOGGER")
    static Logger getLogger(){
        throw new AssertionError();
    }
}
