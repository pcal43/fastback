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

package net.pcal.fastback.mod.fabric;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.pcal.fastback.mod.fabric.mixins.ScreenAccessors;

import java.nio.file.Path;

/**
 * @author pcal
 * @since 0.1.0
 */
final class FabricClientProvider extends BaseFabricProvider implements HudRenderCallback {


    // ======================================================================
    // Constants

    private static final long HUD_TEXT_DURATION = 1000 * 5;

    // ======================================================================
    // Fields

    private MinecraftClient client = null;
    private Text hudText;
    private long hudTextTime;
    private float backupIndicatorAlpha;
    private boolean hudTextShown = false;


    FabricClientProvider() {
    }


    // ====================================================================
    // Public methods

    public void setMinecraftClient(MinecraftClient client) {
        if ((this.client == null) == (client == null)) throw new IllegalStateException();
        this.client = client;
    }

    // ======================================================================
    // MixinGateway implementation

    @Override
    public void renderMessageScreen(DrawContext drawContext, float tickDelta) {
        onHudRender(drawContext, tickDelta);
    }

    // ====================================================================
    // FrameworkProvider implementation

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public void setHudText(Text text) {
        if (text == null) {
            this.hudTextShown = false;
        } else {
            this.hudText = text; // so the hud renderer can find it
            this.hudTextShown = true;
        }
    }

    @Override
    public void setMessageScreenText(Text text) {
        final Screen screen = client.currentScreen;
        if (screen instanceof MessageScreen) {
            ((ScreenAccessors) screen).setTitle(text);
        }
    }

    @Override
    public Path getSavesDir() {
        return FabricLoader.getInstance().getGameDir().resolve("saves");
    }

    // ====================================================================
    // HudRenderCallback implementation

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        if (this.hudText == null) return;
        if (!this.client.options.getShowAutosaveIndicator().getValue()) return;

        final long delta = System.currentTimeMillis() - this.hudTextTime;
        final float alpha = MathHelper.lerp(delta, this.hudTextTime, this.hudTextTime + HUD_TEXT_DURATION);
        if (alpha > 1) {
            hudText = null;
            return;
        }
        final float clamped = MathHelper.clamp(alpha, 0.0F, 1.0F);
        int i = Math.floor(255.0F * clamped);
        final TextRenderer textRenderer = this.client.textRenderer;
        // int j = textRenderer.getWidth(this.hudText);
        int k = 16777215 | i << 24 & -16777216;
        // int scaledWidth = this.client.getWindow().getScaledWidth();
        int x = 2; //scaledWidth - j - 5;
        int y = 2;
        drawContext.drawTextWithShadow(textRenderer, this.hudText, x, y, k);
    }
}
