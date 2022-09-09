package net.pcal.gitback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.gitback.ModContext;

import static java.util.Objects.requireNonNull;
import static net.pcal.gitback.commands.Commands.SUCCESS;
import static net.pcal.gitback.commands.Commands.executeStandard;

public class UuidCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> argb, ModContext ctx) {
        final UuidCommand rc = new UuidCommand(ctx);
        argb.then(CommandManager.literal("uuid").executes(rc::execute));
    }

    private final ModContext ctx;

    private UuidCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, tl) -> {
            tl.feedback(wc.worldUuid());
            return SUCCESS;
        });
    }
}
