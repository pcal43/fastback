package net.pcal.fastback.mod.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.pcal.fastback.logging.UserMessage;

import static net.pcal.fastback.commands.Commands.createBackupCommand;

/**
 * @author pcal
 * @since 0.16.0
 */
class ForgeCommonProvider extends BaseForgeProvider {

    ForgeCommonProvider() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onDedicatedServerStartupEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStartupEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStoppingEvent);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommandEvent);
    }


    // ======================================================================
    // Event handlers

    private void onDedicatedServerStartupEvent(FMLDedicatedServerSetupEvent event) {
        this.onInitialize();
    }

    private void onServerStartupEvent(ServerStartedEvent event) {
        this.onWorldStart(event.getServer());
    }

    private void onServerStoppingEvent(ServerStoppingEvent event) {
        this.onWorldStop();
    }

    private void onRegisterCommandEvent(RegisterCommandsEvent event) {
        final CommandDispatcher<ServerCommandSource> commandDispatcher = event.getDispatcher();
        final LiteralArgumentBuilder<ServerCommandSource> backupCommand =
                createBackupCommand(permName -> x -> true);
        commandDispatcher.register(backupCommand);
    }

    /**
     TODO This one isn't it.  We need to hear about it when an autosaves (and only autosaves) are completed.
     Might have to delve into Forge mixins to do this.
     private void onLevelSaveEvent(LevelEvent.Save event) {
     provider.onAutoSaveComplete();
     }
     **/


    // ======================================================================
    // Fastback MinecraftProvider implementation

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public void setHudText(UserMessage userMessage) {
    }

    @Override
    public void clearHudText() {
    }

    @Override
    public void setMessageScreenText(UserMessage userMessage) {
    }

    @Override
    void renderOverlayText(DrawContext drawContext) {
    }
}
