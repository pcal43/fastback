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

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import static net.pcal.fastback.common.mod.Mod.mod;
import static net.pcal.fastback.neoforge.NeoForgeLoaderHelper.NEOFORGE_MOD_ID;

/**
 * NeoForge mod entry point. Handles both dedicated server and client environments.
 *
 * @author pcal
 */
@Mod(NEOFORGE_MOD_ID)
public class NeoForgeModInitializer {

    public NeoForgeModInitializer(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        if (dist == Dist.CLIENT) {
            NeoForgeClientInitializer.init(modEventBus);
        } else {
            net.pcal.fastback.common.mod.Mod.initializeForDedicatedServer(new NeoForgeLoaderHelper(false));
            NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) ->
                    mod().onWorldStart(event.getServer()));
            NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) ->
                    mod().onWorldStop());
        }
    }
}
