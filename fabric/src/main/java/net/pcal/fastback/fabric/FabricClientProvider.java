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
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.pcal.fastback.common.logging.UserMessage;
import net.pcal.fastback.common.mixins.ScreenAccessors;
import net.pcal.fastback.common.mod.ClientHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.pcal.fastback.common.logging.SystemLogger.syslog;
import static net.pcal.fastback.common.mod.UserMessageUtil.messageToText;

/**
 * Fabric implementation of {@link ClientHelper}. Handles HUD rendering, message screen
 * injection, saves directory, and mods backup paths. Client-side only.
 *
 * @author pcal
 * @since 0.1.0
 */
final class FabricClientProvider implements ClientHelper, HudRenderCallback {

    // ======================================================================
    // Constants

    private static final long TEXT_TIMEOUT = 10 * 1000;

    // ======================================================================
    // Fields

    private Minecraft client = null;
    private Component hudText;
    private long hudTextTime;

    // ======================================================================
    // Package-private lifecycle

    public void setMinecraftClient(Minecraft client) {
        if ((this.client == null) == (client == null)) throw new IllegalStateException();
        this.client = client;
    }

    // ======================================================================
    // ClientHelper implementation

    @Override
    public Path getSavesDir() {
        return FabricLoader.getInstance().getGameDir().resolve("saves");
    }

    @Override
    public Collection<Path> getModsBackupPaths() {
        final Path gameDir = FabricLoader.getInstance().getGameDir();
        final List<Path> out = new ArrayList<>();
        out.add(gameDir.resolve("options.txt"));
        out.add(gameDir.resolve("mods"));
        out.add(gameDir.resolve("config"));
        out.add(gameDir.resolve("resourcepacks"));
        return out;
    }

    @Override
    public void setHudText(UserMessage userMessage) {
        if (userMessage == null) {
            clearHudText();
        } else {
            this.hudText = messageToText(userMessage);
            this.hudTextTime = System.currentTimeMillis();
        }
    }

    @Override
    public void clearHudText() {
        this.hudText = null;
    }

    @Override
    public void setMessageScreenText(UserMessage userMessage) {
        if (this.client == null) return;
        final Screen screen = client.screen;
        if (screen instanceof GenericMessageScreen) {
            ((ScreenAccessors) screen).setTitle(messageToText(userMessage));
        }
    }

    @Override
    public void renderMessageScreen(GuiGraphics guiGraphics) {
        renderHud(guiGraphics);
    }

    // ======================================================================
    // HudRenderCallback implementation

    @Override
    public void onHudRender(GuiGraphics drawContext, DeltaTracker tickDelta) {
        renderHud(drawContext);
    }

    // ======================================================================
    // Private

    private void renderHud(GuiGraphics guiGraphics) {
        if (this.client == null) return;
        if (this.hudText == null) return;
        if (!this.client.options.showAutosaveIndicator().get()) return;
        if (System.currentTimeMillis() - this.hudTextTime > TEXT_TIMEOUT) {
            this.hudText = null;
            syslog().debug("hud text timed out.  somebody forgot to clean up");
            return;
        }
        guiGraphics.drawString(this.client.font, this.hudText, 2, 2, 1);
    }
}
