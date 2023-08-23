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

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.pcal.fastback.logging.Log4jLogger;
import net.pcal.fastback.mod.FrameworkServiceProvider;
import net.pcal.fastback.mod.LifecycleListener;
import org.apache.logging.log4j.LogManager;

import static net.pcal.fastback.mod.fabric.BaseFabricProvider.MOD_ID;

/**
 * Initializer that runs in a dedicated server.
 *
 * @author pcal
 * @since 0.0.1
 */
public class FabricServerInitializer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        final BaseFabricProvider serverProvider = new FabricServerProvider();
        final LifecycleListener lifecycle = FrameworkServiceProvider.register(serverProvider,
                new Log4jLogger(LogManager.getLogger(MOD_ID)));
        MixinGateway.Singleton.register(serverProvider);
        ServerLifecycleEvents.SERVER_STARTING.register(
                minecraftServer -> {
                    serverProvider.setMinecraftServer(minecraftServer);
                    lifecycle.onWorldStart();
                }
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(
                minecraftServer -> {
                    try {
                        lifecycle.onWorldStop();
                    } finally {
                        serverProvider.setMinecraftServer(null);
                    }
                }
        );
        lifecycle.onInitialize();
    }
}
