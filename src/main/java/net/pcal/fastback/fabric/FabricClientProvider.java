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

package net.pcal.fastback.fabric;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.pcal.fastback.fabric.mixins.ScreenAccessors;
import net.pcal.fastback.logging.Message;

import java.nio.file.Path;

/**
 * @author pcal
 * @since 0.1.0
 */
final class FabricClientProvider extends FabricProvider implements HudRenderCallback {

    private MinecraftClient client = null;
    private Text statusText;

    FabricClientProvider() {
    }

    // ====================================================================
    // Public methods

    public void setMinecraftClient(MinecraftClient client) {
        if ((this.client == null) == (client == null)) throw new IllegalStateException();
        this.client = client;
    }

    // ====================================================================
    // FrameworkProvider implementation

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public void setHudText(Message message) {
        if (message == null) {
            this.statusTextShown = false;
        } else {
            this.statusText = messageToText(message);
            this.statusTextShown = true;
        }
    }

    @Override
    public void setClientSavingScreenText(Message message) {
        final Screen screen = client.currentScreen;
        if (screen instanceof MessageScreen) {
            ((ScreenAccessors) screen).setTitle(messageToText(message));
        }
    }

    @Override
    public void sendClientChatMessage(Message message) {
        if (this.client != null) {
            client.inGameHud.getChatHud().addMessage(messageToText(message));
        }
    }

    @Override
    public Path getSnapshotRestoreDir() {
        return FabricLoader.getInstance().getGameDir().resolve("saves");
    }

    // ====================================================================
    // HudRender implementation


    private float backupIndicatorAlpha;
    private float lastBackupIndicatorAlpha;
    private boolean statusTextShown = false;

    @Override
    public void onHudRender(MatrixStack matrixStack, float tickDelta) {
        if (this.statusText == null) return;
        this.lastBackupIndicatorAlpha = this.backupIndicatorAlpha;
        this.backupIndicatorAlpha = MathHelper.lerp(0.2F, this.backupIndicatorAlpha, statusTextShown ? 1.0F : 0.0F);

        if (this.client.options.getShowAutosaveIndicator().getValue() && (this.backupIndicatorAlpha > 0.0F || this.lastBackupIndicatorAlpha > 0.0F)) {
            int i = MathHelper.floor(255.0F * MathHelper.clamp(MathHelper.lerp(this.client.getTickDelta(), this.lastBackupIndicatorAlpha, this.backupIndicatorAlpha), 0.0F, 1.0F));

            if (i > 8) {
                MatrixStack matrices = new MatrixStack();
                TextRenderer textRenderer = this.client.textRenderer;
                int j = textRenderer.getWidth(this.statusText);
                int k = 16777215 | i << 24 & -16777216;
                int scaledWidth = this.client.getWindow().getScaledWidth();
                int scaledHeight = this.client.getWindow().getScaledHeight();
                textRenderer.drawWithShadow(matrices, this.statusText, (float) (scaledWidth - j - 10), (float) (scaledHeight - 15), k);
            } else {
                statusText = null;
            }
        }
    }
}
