/*
 * FastBack - Fast, incremental Minecraft backups powered by Git.
 * Copyright (C) 2026 pcal.net
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.pcal.fastback.common.mod.ClientHelper;
import net.pcal.fastback.common.mod.Mod;

import static net.pcal.fastback.common.mod.Mod.mod;

/**
 * Client-side NeoForge initialization. Kept separate from NeoForgeModInitializer
 * so that client-only classes are not classloaded on a dedicated server.
 *
 * @author pcal
 */
class NeoForgeClientInitializer {

    static void init(IEventBus modEventBus) {
        final boolean[] initialized = {false};
        // We need the Minecraft client instance to build ClientHelper, so we defer until the first tick.
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> {
            if (!initialized[0]) {
                initialized[0] = true;
                final Minecraft client = Minecraft.getInstance();
                Mod.initializeForClient(new NeoForgeLoaderHelper(true), new ClientHelper(client));
            }
        });
        NeoForge.EVENT_BUS.addListener((RenderGuiLayerEvent.Post event) ->
                mod().renderHud(event.getGuiGraphics()));
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) ->
                mod().onWorldStart(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) ->
                mod().onWorldStop());
    }
}

