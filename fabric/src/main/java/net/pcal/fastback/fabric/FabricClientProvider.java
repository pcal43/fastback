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
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.pcal.fastback.common.mod.ClientHelper;

/**
 * Fabric client-side hook. All client logic lives in {@link ClientHelper}.
 * Loader-specific methods (getSavesDir, getModsBackupPaths) are provided
 * by the LoaderHelper passed to ModImpl.
 *
 * @author pcal
 * @since 0.1.0
 */
final class FabricClientProvider extends ClientHelper implements HudRenderCallback {

    @Override
    public void onHudRender(GuiGraphics drawContext, DeltaTracker tickDelta) {
        renderHud(drawContext);
    }
}
