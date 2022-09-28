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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.pcal.fastback.LifecycleUtils;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.fabric.mixins.ScreenAccessors;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * Initializer that runs in a client.
 *
 * @author pcal
 * @since 0.0.1
 */
public class FabricClientModInitializer implements ClientModInitializer {


    @Override
    public void onInitializeClient() {
        final FabricServiceProvider fabricProvider = FabricServiceProvider.create();
        fabricProvider.setClientProvider(new FabricClientProviderImpl(MinecraftClient.getInstance()));
        final ModContext modContext = ModContext.create(fabricProvider);

        ClientLifecycleEvents.CLIENT_STARTED.register(
                minecraftClient -> {
                    final FabricClientProvider fcp = new FabricClientProviderImpl(MinecraftClient.getInstance());
                    fabricProvider.setClientProvider(fcp);
                }
        );
        ClientLifecycleEvents.CLIENT_STOPPING.register(
                minecraftClient -> {
                    LifecycleUtils.onTermination(modContext);
                    fabricProvider.setClientProvider(null);
                }
        );

        ServerLifecycleEvents.SERVER_STARTING.register(
                minecraftServer -> {
                    fabricProvider.setMinecraftServer(minecraftServer);
                    LifecycleUtils.onWorldStart(modContext);
                }
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(
                minecraftServer -> {
                    LifecycleUtils.onWorldStop(modContext);
                    fabricProvider.setMinecraftServer(null);
                }
        );
        LifecycleUtils.onInitialize(modContext);
    }

    private static class FabricClientProviderImpl implements FabricClientProvider {

        private final MinecraftClient client;

        private FabricClientProviderImpl(MinecraftClient client) {
            this.client = requireNonNull(client);
        }

        @Override
        public void consumeSaveScreenText(Text text) {
            final Screen screen = client.currentScreen;
            if (screen instanceof MessageScreen) {
                ((ScreenAccessors) screen).setTitle(text);
            }
        }

        @Override
        public Path getClientRestoreDir() {
            return FabricLoader.getInstance().getGameDir().resolve("saves");
        }

        @Override
        public void sendClientChatMessage(Text text) {
            client.inGameHud.getChatHud().addMessage(text);
        }

        @Override
        public void renderBackupIndicator(Text text) {
            if (true || this.client.options.getShowAutosaveIndicator().getValue()) {
                MatrixStack matrices = new MatrixStack();
                TextRenderer textRenderer = this.client.textRenderer;
                int j = textRenderer.getWidth(text);
                int k = 16777215;
                int scaledWidth = this.client.getWindow().getScaledWidth();
                int scaledHeight = this.client.getWindow().getScaledHeight();

                textRenderer.drawWithShadow(matrices, text, (float)(scaledWidth - j - 10), (float)(scaledHeight - 15), k);
            }
        }
    }
}
