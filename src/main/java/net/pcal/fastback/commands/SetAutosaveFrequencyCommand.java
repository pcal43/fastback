package net.pcal.fastback.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.lib.StoredConfig;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.ModContext.ExecutionLock.WRITE_CONFIG;
import static net.pcal.fastback.commands.Commands.*;
import static net.pcal.fastback.logging.Message.localized;

public class SetAutosaveFrequencyCommand {
    private static final String COMMAND_NAME = "set-autosave-frequency";

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final LiteralArgumentBuilder<ServerCommandSource> setCommand = literal(COMMAND_NAME).
                requires(subcommandPermission(ctx, COMMAND_NAME));
        setCommand.then(argument("frequency", IntegerArgumentType.integer(1)).executes(context -> execute(ctx, context.getSource(), IntegerArgumentType.getInteger(context,"frequency"))));
        argb.then(setCommand);
    }

    public static int execute(final ModContext ctx, final ServerCommandSource scs, int frequency) {
        final Logger log = commandLogger(ctx, scs);
        gitOp(ctx, WRITE_CONFIG, log, git -> {
            final StoredConfig config = git.getRepository().getConfig();
            WorldConfig.setAutosaveFrequency(config, frequency);
            config.save();
            log.notify(localized("fastback.notify.info-autosave-frequency", frequency));
        });
        return SUCCESS;
    }
}
