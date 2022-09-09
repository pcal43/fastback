package net.pcal.gitback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.gitback.ModContext;
import net.pcal.gitback.WorldConfig;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.gitback.commands.Commands.FAILURE;
import static net.pcal.gitback.commands.Commands.SUCCESS;
import static net.pcal.gitback.commands.Commands.executeStandard;

public class ShutdownCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final ShutdownCommand c = new ShutdownCommand(ctx);
        argb.then(
                literal("shutdown").executes(c::show).then(
                        literal("enable").executes(c::enable)).then(
                        literal("disable").executes(c::disable))
        );
    }

    private final ModContext ctx;

    private ShutdownCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int show(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, tali) -> {
            final boolean enabled = wc.isShutdownBackupEnabled();
            if (enabled) {
                tali.feedback("Backup on shutdown is currently enabled.");
            } else {
                tali.feedback("Backup on shutdown is currently disabled.");
            }
            return SUCCESS;
        });
    }

    private int enable(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, tali) -> {
            final boolean enabled = wc.isShutdownBackupEnabled();
            if (enabled) {
                tali.error("Backup on world shutdown is already enabled.");
                return FAILURE;
            } else {
                WorldConfig.setShutdownBackupEnabled(gitc, true);
                gitc.save();
                tali.feedback("Backup on world shutdown enabled.");
                return SUCCESS;
            }
        });
    }

    private int disable(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, tali) -> {
            final boolean enabled = wc.isShutdownBackupEnabled();
            if (!enabled) {
                tali.error("Backup on shutdown is already disabled.");
                return FAILURE;
            } else {
                WorldConfig.setShutdownBackupEnabled(gitc, false);
                gitc.save();
                tali.feedback("Backup on world shutdown disabled.");
                return SUCCESS;
            }
        });
    }
}
