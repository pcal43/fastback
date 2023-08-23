/*
 * FastBack - Fast, incremental Minecraft backups powered by Git.
 * Copyright (C) 2022 pcal.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package net.pcal.fastback.mod.fabric.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.MessageScreen;
import net.pcal.fastback.mod.fabric.BaseFabricProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author pcal
 * @since 0.0.1
 */
@Mixin(MessageScreen.class)
public class MessageScreenMixin {

    /**
     * Apply filtering behavior to free floating entities above the hopper.
     */
    @Inject(method = "render", at = @At("TAIL"))
    public void __render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BaseFabricProvider.getInstance().renderMessageScreen(context, delta);
    }
}
