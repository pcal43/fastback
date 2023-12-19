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

import net.minecraft.client.gui.GuiGraphics;

/**
 * Singleton 'gateway' that mixin code goes through to call back into the mod.
 *
 * @author pcal
 * @since 0.13.1
 */
public interface MixinGateway {

    static MixinGateway get() {
        return Singleton.INSTANCE;
    }

    boolean isWorldSaveEnabled();

    void autoSaveCompleted();

    void renderMessageScreen(GuiGraphics drawContext, float tickDelta);

    class Singleton {
        private static MixinGateway INSTANCE = null;

        public static void register(MixinGateway gateway) {
            Singleton.INSTANCE = gateway;
        }
    }
}
