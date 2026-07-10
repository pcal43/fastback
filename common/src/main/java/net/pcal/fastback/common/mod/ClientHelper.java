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

package net.pcal.fastback.common.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.pcal.fastback.common.logging.UserMessage;
import net.pcal.fastback.common.mixins.ScreenAccessors;

import static net.pcal.fastback.common.logging.SystemLogger.syslog;
import static net.pcal.fastback.common.mod.UserMessageUtil.messageToText;

/**
 * Client-only helper services. Holds vanilla Minecraft client state and provides
 * concrete implementations for HUD and message screen management.
 *
 * @author pcal
 * @since 0.2.0
 */
public final class ClientHelper {

    // ======================================================================
    // Constants

    private static final long TEXT_TIMEOUT = 10 * 1000;

    // ======================================================================
    // Fields

    private final Minecraft client;
    private Component hudText;
    private long hudTextTime;

    // ======================================================================
    // Constructor

    public ClientHelper(Minecraft client) {
        this.client = client;
    }

    // ======================================================================
    // Concrete — vanilla Minecraft implementations

    public void setHudText(UserMessage userMessage) {
        if (userMessage == null) {
            clearHudText();
        } else {
            this.hudText = messageToText(userMessage);
            this.hudTextTime = System.currentTimeMillis();
        }
    }

    public void clearHudText() {
        this.hudText = null;
    }

    public void setMessageScreenText(UserMessage userMessage) {
        if (this.client == null) return;
        final Screen screen = client.gui.screen();
        if (screen instanceof GenericMessageScreen) {
            ((ScreenAccessors) screen).setTitle(messageToText(userMessage));
            ((ScreenAccessors) screen).invokeRebuildWidgets(); // force it to rebuild the message component with the new title
        }
    }

    public void renderMessageScreen(GuiGraphicsExtractor guiGraphics) {
        renderHud(guiGraphics);
    }

    public void renderHud(GuiGraphicsExtractor guiGraphics) {
        if (this.client == null) return;
        if (this.hudText == null) return;
        if (!this.client.options.showAutosaveIndicator().get()) return;
        if (System.currentTimeMillis() - this.hudTextTime > TEXT_TIMEOUT) {
            this.hudText = null;
            syslog().debug("hud text timed out.  somebody forgot to clean up");
            return;
        }
        guiGraphics.text(this.client.font, this.hudText, 2, 2, 0xFFFFFF, false);
    }
}
