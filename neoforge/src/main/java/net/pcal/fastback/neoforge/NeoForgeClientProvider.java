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
package net.pcal.fastback.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mixins.ScreenAccessors;

import java.nio.file.Path;

import static net.pcal.fastback.mod.MinecraftProvider.messageToText;

public class NeoForgeClientProvider extends BaseNeoForgeProvider {
    private static final long TEXT_TIMEOUT = 10 * 1000;
    private Minecraft client = null;
    private Component hudText;
    private long hudTextTime;

    public void setMinecraftClient(Minecraft client) {
        if ((this.client == null) == (client == null))
            throw new IllegalStateException();
        this.client = client;
    }

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    public Path getSavesDir() {
        return FMLPaths.GAMEDIR.get().resolve("saves");
    }

    @Override
    public void setHudText(UserMessage userMessage) {
        if (userMessage == null) {
            clearHudText();
            return;
        }
        this.hudText = messageToText(userMessage);
        this.hudTextTime = System.currentTimeMillis();
    }

    @Override
    public void clearHudText() {
        this.hudText = null;
    }

    @Override
    public void setMessageScreenText(UserMessage message) {
        if (this.client == null) return;
        final Screen currentScreen = this.client.screen;
        if (currentScreen instanceof GenericMessageScreen) {
            ((ScreenAccessors) currentScreen).setTitle(messageToText(message));
        }
    }

    @Override
    public void renderMessageScreen(GuiGraphics guiGraphics) {
        renderHud(guiGraphics);
    }

    void renderHud(GuiGraphics guiGraphics) {
        if (this.hudText == null) return;
        if (System.currentTimeMillis() - this.hudTextTime > TEXT_TIMEOUT) {
            this.hudText = null;
            return;
        }
        if (this.client == null) return;
        final int x = 3;
        final int y = this.client.getWindow().getGuiScaledHeight() - 20;
        guiGraphics.drawString(this.client.font, this.hudText, x, y, 0xFFFFFF);
    }
}
