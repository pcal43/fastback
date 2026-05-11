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
package net.pcal.fastback.mod.neoforge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.pcal.fastback.mod.LifecycleListener;
/**
 * Client-side NeoForge initialization. Kept separate from NeoForgeModInitializer
 * so that client-only classes are not classloaded on a dedicated server.
 *
 * @author pcal
 */
class NeoForgeClientInitializer {
    static void init(IEventBus modEventBus) {
        final NeoForgeClientProvider clientProvider = new NeoForgeClientProvider();
        final LifecycleListener lifecycle = clientProvider.initialize();
        NeoForge.EVENT_BUS.addListener((RenderGuiLayerEvent.Post event) ->
                clientProvider.renderHud(event.getGuiGraphics()));
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) -> {
            clientProvider.setMinecraftServer(event.getServer());
            lifecycle.onWorldStart();
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            try {
                lifecycle.onWorldStop();
            } finally {
                clientProvider.setMinecraftServer(null);
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                clientProvider.onRegisterCommands(event));
    }
}
