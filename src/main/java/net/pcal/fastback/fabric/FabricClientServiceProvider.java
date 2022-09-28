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

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;

import net.pcal.fastback.fabric.mixins.ScreenAccessors;
import net.pcal.fastback.logging.Message;

import java.io.IOException;
import java.nio.file.Path;


final class FabricClientServiceProvider extends FabricServiceProvider {

    private MinecraftClient client = null;

    FabricClientServiceProvider() {}

    public void setMinecraftClient(MinecraftClient client) {
        if ((this.client == null) == (client == null)) throw new IllegalStateException();
        this.client = client;
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
    public boolean isClient() {
        return false;
    }

    @Override
    public Path getClientSavesDir() {
        return FabricLoader.getInstance().getGameDir().resolve("saves");
    }

    @Override
    public void renderBackupIndicator(Message message) {
        /**
         if (true || this.client.options.getShowAutosaveIndicator().getValue()) {
         MatrixStack matrices = new MatrixStack();
         TextRenderer textRenderer = this.client.textRenderer;
         int j = textRenderer.getWidth(text);
         int k = 16777215;
         int scaledWidth = this.client.getWindow().getScaledWidth();
         int scaledHeight = this.client.getWindow().getScaledHeight();

         textRenderer.drawWithShadow(matrices, text, (float)(scaledWidth - j - 10), (float)(scaledHeight - 15), k);
         }
         **/
    }
}
