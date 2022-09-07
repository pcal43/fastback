package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.pcal.fastback.ModContext;

import static java.util.Objects.requireNonNull;

public class RestoreCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, ModContext ctx) {
        final RestoreCommand rc = new RestoreCommand(ctx);
        fastbackCmd.then(CommandManager.literal("restore").executes(rc::execute));
    }

    private final ModContext context;

    private RestoreCommand(ModContext context) {
        this.context = requireNonNull(context);
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        cc.getSource().sendFeedback(Text.literal("Restoring from backup!"), true);
        ModContext.WorldContext world = this.context.getWorldContext(cc.getSource().getServer());
        return 1;
    }
}
