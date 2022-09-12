package net.pcal.fastback.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.pcal.fastback.LifecycleUtils;
import net.pcal.fastback.fabric.mixins.ScreenAccessors;

public class FastbackClientModInitializer implements ClientModInitializer {

    private final FabricModContext modContext = new FabricModContext();

    @Override
    public void onInitializeClient() {
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
        LifecycleUtils.onMinecraftStart(modContext);

        this.modContext.installSaveScreenhandler(
                text -> {
                    final MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null) {
                        final Screen screen = client.currentScreen;
                        if (screen instanceof MessageScreen) {
                            ((ScreenAccessors) screen).setTitle(text);
                        }
                    }
                });
    }
}
