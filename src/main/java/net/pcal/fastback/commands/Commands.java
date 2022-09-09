package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;

public class Commands {

    static int FAILURE = 0;
    static int SUCCESS = 1;

    public static void registerCommands(final ModContext ctx, final String fastbackCommand) {
        final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd = LiteralArgumentBuilder.literal(fastbackCommand);
        EnableCommand.register(fastbackCmd, ctx);
        DisableCommand.register(fastbackCmd, ctx);
        StatusCommand.register(fastbackCmd, ctx);
        RestoreCommand.register(fastbackCmd, ctx);
        ListCommand.register(fastbackCmd, ctx);
        RemoteCommand.register(fastbackCmd, ctx);
        CreateFileRemoteCommand.register(fastbackCmd, ctx);
        ShutdownCommand.register(fastbackCmd, ctx);
        UuidCommand.register(fastbackCmd, ctx);
        VersionCommand.register(fastbackCmd, ctx);
        NowCommand.register(fastbackCmd, ctx);
        CommandRegistrationCallback.EVENT.register((dispatcher, regAccess, env) -> dispatcher.register(fastbackCmd));
    }
}
