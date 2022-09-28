package net.pcal.fastback.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.lib.StoredConfig;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.ModContext.ExecutionLock.WRITE_CONFIG;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.Message.localized;

public class SetAutosaveWaitCommand {

    private static final String COMMAND_NAME = "set-autosave-wait";
    private static final String ARGUMENT = "wait";

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).then(
                                argument(ARGUMENT, IntegerArgumentType.integer(0)).
                                        executes(cc -> setWait(ctx, cc))
                        )
        );
    }

    public static int setWait(final ModContext ctx, final CommandContext<ServerCommandSource> cc) {
        final Logger log = commandLogger(ctx, cc.getSource());
        gitOp(ctx, WRITE_CONFIG, log, git -> {
            final int wait = cc.getArgument(ARGUMENT, int.class);
            final StoredConfig config = git.getRepository().getConfig();
            WorldConfig.setAutosaveWait(config, wait);
            config.save();
            log.notify(localized("fastback.notify.info-autosave-wait", wait));
        });
        return SUCCESS;
    }
}
