package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;

public class Commands {

    public static void registerCommands(String fastbackCommand) {
        final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd = LiteralArgumentBuilder.literal(fastbackCommand);
        RestoreCommand.register(fastbackCmd);
        CommandRegistrationCallback.EVENT.register((dispatcher, regAccess, env) -> dispatcher.register(fastbackCmd));
    }

}
