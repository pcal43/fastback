package net.pcal.fastback.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.pcal.fastback.LifecycleUtils;
import net.pcal.fastback.ModContext;

public class FastbackDedicatedServerModInitializer implements DedicatedServerModInitializer {

    private final ModContext modContext = ModContext.create(new FabricFrameworkProvider());

    @Override
    public void onInitializeServer() {
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
