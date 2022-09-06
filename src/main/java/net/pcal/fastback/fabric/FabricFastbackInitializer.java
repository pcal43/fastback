package net.pcal.fastback.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.pcal.fastback.LifecycleUtils;

public class FabricFastbackInitializer implements ModInitializer {

    // ===================================================================================
    // ModInitializer implementation

    @Override
    public void onInitialize() {
        final FabricModContext modContext = new FabricModContext();

        ServerLifecycleEvents.SERVER_STOPPED.register(
                minecraftServer->{
                    LifecycleUtils.onWorldStop(new FabricWorldContext(modContext, minecraftServer));
                }
        );
        ServerLifecycleEvents.SERVER_STARTING.register(
                minecraftServer->{
                    LifecycleUtils.onWorldStart(new FabricWorldContext(modContext, minecraftServer));
                }
        );
        LifecycleUtils.onMinecraftStart(modContext);
    }
}
