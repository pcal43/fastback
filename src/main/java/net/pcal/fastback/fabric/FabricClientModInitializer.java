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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.pcal.fastback.LifecycleUtils;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.fabric.mixins.ScreenAccessors;

import java.nio.file.Path;

public class FabricClientModInitializer implements ClientModInitializer {

    private final FabricServiceProvider fabricProvider = FabricServiceProvider.forClient(new FabricClientProviderImpl());
    private final ModContext modContext = ModContext.create(fabricProvider);

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(
                minecraftClient -> {
                    this.modContext.getLogger().info("CLIENT STARTED");
                }
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(
                minecraftServer -> {
                    LifecycleUtils.onWorldStop(modContext, minecraftServer);
                }
        );
        ServerLifecycleEvents.SERVER_STARTING.register(
                minecraftServer -> {
                    LifecycleUtils.onWorldStart(modContext, minecraftServer);
                }
        );
        LifecycleUtils.onClientStart(modContext);
    }

    private static class FabricClientProviderImpl implements FabricClientProvider {

        @Override
        public void consumeSaveScreenText(Text text) {
            final MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                final Screen screen = client.currentScreen;
                if (screen instanceof MessageScreen) {
                    ((ScreenAccessors) screen).setTitle(text);
                }
            }
        }

        @Override
        public Path getClientRestoreDir() {
            return null;
        }

        @Override
        public void sendClientChatMessage(Text text) {
            final MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.inGameHud.getChatHud().addMessage(text);
            }
        }
    }
}
