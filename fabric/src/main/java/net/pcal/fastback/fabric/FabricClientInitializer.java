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

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.pcal.fastback.common.mod.ClientHelper;
import net.pcal.fastback.common.mod.Mod;

/**
 * Initializer that runs on the client (both integrated and dedicated-server-from-client).
 *
 * @author pcal
 * @since 0.0.1
 */
public class FabricClientInitializer implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(
                minecraftClient -> {
                    Mod.initializeForClient(new FabricLoaderHelper(), new ClientHelper(minecraftClient));
                    HudRenderCallback.EVENT.register((guiGraphics,deltaTracker)->{
                        Mod.mod().renderHud(guiGraphics);
                    });
                }
        );
        ServerLifecycleEvents.SERVER_STARTING.register(
                minecraftServer -> Mod.mod().onWorldStart(minecraftServer)
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(
                minecraftServer -> Mod.mod().onWorldStop()
        );
    }
}
