package net.pcal.fastback.mod.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.commands.Commands.createBackupCommand;


/**
 * @author pcal
 * @since 0.16.0
 */
@Mod("fastback")
public class ForgeInitializer {

    private BaseForgeProvider provider = null;

    public ForgeInitializer() {
        sanityTest();
        // REVIEW this works but is it right?  I find the Forge event system somewhat vexing
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onClientStartupEvent);
        modEventBus.addListener(this::onDedicatedServerStartupEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStartupEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStoppingEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommandEvent);
    }

    // ======================================================================
    // Event handlers

    private void onClientStartupEvent(FMLClientSetupEvent event) {
        if (provider != null) throw new IllegalStateException();
        provider = new ForgeClientProvider();
        provider.onInitialize();
    }

    void onDedicatedServerStartupEvent(FMLDedicatedServerSetupEvent event) {
        if (provider != null) throw new IllegalStateException();
        provider = new ForgeDedicatedServerProvider();
        provider.onInitialize();
    }

    void onServerStartupEvent(ServerStartedEvent event) {
        requireNonNull(provider, "PROVIDER_SINGLETON was never set").onWorldStart(event.getServer());
    }

    void onServerStoppingEvent(ServerStoppingEvent event) {
        requireNonNull(provider, "PROVIDER_SINGLETON was never set").onWorldStop();
    }

    void onRegisterCommandEvent(RegisterCommandsEvent event) {
        final CommandDispatcher<ServerCommandSource> commandDispatcher = event.getDispatcher();
        final LiteralArgumentBuilder<ServerCommandSource> backupCommand =
                createBackupCommand(permName -> x -> true);
        commandDispatcher.register(backupCommand);
    }

    // ======================================================================
    // Private

    /**
     * Fail fast on basic classpath issues.
     */
    private void sanityTest() {
        try {
            Class.forName("org.eclipse.jgit.api.errors.GitAPIException");
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}

