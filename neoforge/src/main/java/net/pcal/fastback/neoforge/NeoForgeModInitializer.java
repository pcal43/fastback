package net.pcal.fastback.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.pcal.fastback.mod.LifecycleListener;

import static net.pcal.fastback.neoforge.BaseNeoForgeProvider.MOD_ID;

/**
 * NeoForge mod entry point. Handles both dedicated server and client environments.
 *
 * @author pcal
 */
@Mod(MOD_ID)
public class NeoForgeModInitializer {

    public NeoForgeModInitializer(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        if (dist == Dist.CLIENT) {
            NeoForgeClientInitializer.init(modEventBus);
        } else {
            final NeoForgeServerProvider serverProvider = new NeoForgeServerProvider();
            final LifecycleListener lifecycle = serverProvider.initialize();

            NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) -> {
                serverProvider.setMinecraftServer(event.getServer());
                lifecycle.onWorldStart();
            });
            NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
                try {
                    lifecycle.onWorldStop();
                } finally {
                    serverProvider.setMinecraftServer(null);
                }
            });
            NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                    serverProvider.onRegisterCommands(event));
        }
    }
}

