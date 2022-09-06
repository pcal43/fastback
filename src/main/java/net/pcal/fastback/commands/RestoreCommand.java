package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class RestoreCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> fastbackCmd) {
        fastbackCmd.then(CommandManager.literal("restore").executes(
                ctx -> {
                    ctx.getSource().sendFeedback(Text.literal("Restoring from backup!"), true);
                    return 1;
                })
        );
    }
}
