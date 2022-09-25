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

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.pcal.fastback.LifecycleUtils;
import net.pcal.fastback.ModContext;

public class FabricDedicatedServerModInitializer implements DedicatedServerModInitializer {

    private final ModContext modContext = ModContext.create(FabricServiceProvider.forServer());

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(
                minecraftServer -> {
                    LifecycleUtils.onServerStart(modContext);
                    LifecycleUtils.onWorldStart(modContext, minecraftServer);
                }
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(
                minecraftServer -> {
                    LifecycleUtils.onWorldStop(modContext, minecraftServer);
                    LifecycleUtils.onServerStop(modContext);
                }
        );

    }
}
