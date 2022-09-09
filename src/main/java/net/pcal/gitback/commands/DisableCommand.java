package net.pcal.gitback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.gitback.ModContext;
import net.pcal.gitback.WorldConfig;

import static java.util.Objects.requireNonNull;
import static net.pcal.gitback.commands.Commands.SUCCESS;
import static net.pcal.gitback.commands.Commands.executeStandard;

public class DisableCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final DisableCommand rc = new DisableCommand(ctx);
        argb.then(CommandManager.literal("disable").executes(rc::execute));
    }

    private final ModContext ctx;

    private DisableCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, tali) -> {
            WorldConfig.setBackupEnabled(gitc, false);
            WorldConfig.setShutdownBackupEnabled(gitc, false);
            gitc.save();
            tali.feedback("Backups disabled.");
            return SUCCESS;
        });
    }
}
