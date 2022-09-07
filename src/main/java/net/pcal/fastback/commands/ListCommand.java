package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.pcal.fastback.ModContext;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.tasks.ListSnapshotsTask.*;

public class ListCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, final ModContext ctx) {
        final ListCommand rc = new ListCommand(ctx);
        fastbackCmd.then(CommandManager.literal("list").executes(rc::execute));
    }

    private final ModContext mctx;

    private ListCommand(ModContext context) {
        this.mctx = requireNonNull(context);
    }

    private int execute(final CommandContext<ServerCommandSource> cc) {
        final ModContext.WorldContext world = this.mctx.getWorldContext(cc.getSource().getServer());
        final Consumer<String> sink = message -> cc.getSource().sendFeedback(Text.literal(message), false);
        sink.accept("Local snapshots:");
        this.mctx.getExecutorService().execute(listSnapshotsForWorld(world, sink));
        return 1;
    }

}
