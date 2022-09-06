package net.pcal.fastback.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.pcal.fastback.LifecycleUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FabricFastbackInitializer implements ModInitializer {


    // ===================================================================================
    // ModInitializer implementation

    @Override
    public void onInitialize() {

        final FabricModContext ctx = new FabricModContext();
        final Logger log4j = LogManager.getLogger("Fastback");

        ServerLifecycleEvents.SERVER_STOPPED.register(
                minecraftServer->{
                    LifecycleUtils.onWorldStop(new FabricWorldContext(ctx, minecraftServer));
                }
        );
        ServerLifecycleEvents.SERVER_STARTING.register(
                minecraftServer->{
                    LifecycleUtils.onWorldStart(new FabricWorldContext(ctx, minecraftServer));
                }
        );
        LifecycleUtils.onMinecraftStart(ctx);
    }
}
