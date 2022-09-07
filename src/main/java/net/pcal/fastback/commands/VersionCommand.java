package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.tasks.TaskListener;

import static java.util.Objects.requireNonNull;

public class VersionCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, ModContext ctx) {
        final VersionCommand rc = new VersionCommand(ctx);
        fastbackCmd.then(CommandManager.literal("version").executes(rc::execute));
    }

    private final ModContext ctx;

    private VersionCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        final TaskListener tl = new ServerTaskListener(cc.getSource());
        tl.feedback(this.ctx.getFastbackModVersion());
        return 1;
    }
}
