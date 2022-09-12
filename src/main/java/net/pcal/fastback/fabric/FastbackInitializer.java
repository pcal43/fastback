package net.pcal.fastback.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.pcal.fastback.LifecycleUtils;

public class FastbackInitializer implements ModInitializer, DedicatedServerModInitializer {

    // ===================================================================================
    // ModInitializer implementation

    @Override
    public void onInitialize() {
        final FabricModContext modContext = new FabricModContext(EnvType.CLIENT);

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
    }

    // ===================================================================================
    // DedicatedServerModInitializer implementation

    @Override
    public void onInitializeServer() {
        final FabricModContext modContext = new FabricModContext(EnvType.SERVER);

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
    }
}
