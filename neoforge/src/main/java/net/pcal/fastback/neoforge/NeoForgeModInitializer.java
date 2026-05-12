package net.pcal.fastback.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import static net.pcal.fastback.neoforge.NeoForgeLoaderHelper.MOD_ID;

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
            net.pcal.fastback.common.mod.Mod.initializeForDedicatedServer(new NeoForgeLoaderHelper(false));
            NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) ->
                    net.pcal.fastback.common.mod.Mod.mod().onWorldStart(event.getServer()));
            NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) ->
                    net.pcal.fastback.common.mod.Mod.mod().onWorldStop());
        }
    }
}
