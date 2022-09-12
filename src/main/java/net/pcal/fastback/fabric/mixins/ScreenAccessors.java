package net.pcal.fastback.fabric.mixins;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(Screen.class)
public interface ScreenAccessors {

    @Accessor
    @Mutable
    Text getTitle();

    @Accessor
    @Mutable
    void setTitle(Text text);
}
