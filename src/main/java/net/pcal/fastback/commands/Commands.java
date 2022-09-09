package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;

public class Commands {

    static int FAILURE = 0;
    static int SUCCESS = 1;

    public static void registerCommands(final ModContext mctx, final String fastbackCommand) {
        final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd = LiteralArgumentBuilder.literal(fastbackCommand);
        EnableCommand.register(fastbackCmd, mctx);
        DisableCommand.register(fastbackCmd, mctx);
        StatusCommand.register(fastbackCmd, mctx);
        RestoreCommand.register(fastbackCmd, mctx);
        ListCommand.register(fastbackCmd, mctx);
        RemoteCommand.register(fastbackCmd, mctx);
        ShutdownCommand.register(fastbackCmd, mctx);
        UuidCommand.register(fastbackCmd, mctx);
        VersionCommand.register(fastbackCmd, mctx);
        NowCommand.register(fastbackCmd, mctx);
        CommandRegistrationCallback.EVENT.register((dispatcher, regAccess, env) -> dispatcher.register(fastbackCmd));
    }
}
