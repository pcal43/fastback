package net.pcal.fastback.mixins;

import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelStorage.Session.class)
public interface SessionAccessors {
    @Accessor
    LevelStorage.LevelSave getDirectory();
}
