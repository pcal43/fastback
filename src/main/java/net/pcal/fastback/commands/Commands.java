package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;

public class Commands {

    public static void registerCommands(final ModContext mctx, final String fastbackCommand) {
        final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd = LiteralArgumentBuilder.literal(fastbackCommand);
        RestoreCommand.register(fastbackCmd, mctx);
        ListCommand.register(fastbackCmd, mctx);
        CommandRegistrationCallback.EVENT.register((dispatcher, regAccess, env) -> dispatcher.register(fastbackCmd));
    }
}
