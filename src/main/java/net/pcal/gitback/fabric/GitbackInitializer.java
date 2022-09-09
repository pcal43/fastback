package net.pcal.gitback.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.pcal.gitback.LifecycleUtils;

public class GitbackInitializer implements ModInitializer {

    // ===================================================================================
    // ModInitializer implementation

    @Override
    public void onInitialize() {
        final FabricModContext modContext = new FabricModContext();


        ServerLifecycleEvents.SERVER_STOPPED.register(
                minecraftServer -> {
                    LifecycleUtils.onWorldStop(
                            modContext.getWorldSaveDirectory(minecraftServer),
                            modContext.getLogger());
                }
        );
        ServerLifecycleEvents.SERVER_STARTING.register(
                minecraftServer -> {
                    LifecycleUtils.onWorldStart(
                            modContext.getWorldSaveDirectory(minecraftServer),
                            modContext.getLogger());
                }
        );
        LifecycleUtils.onMinecraftStart(modContext);
    }


}
