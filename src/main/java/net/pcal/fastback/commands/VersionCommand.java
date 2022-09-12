package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.executeStandard;

public class VersionCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> argb, ModContext ctx) {
        final VersionCommand rc = new VersionCommand(ctx);
        argb.then(CommandManager.literal("version").executes(rc::execute));
    }

    private final ModContext ctx;

    private VersionCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            log.notify(this.ctx.getModVersion());
            return SUCCESS;
        });
    }
}
