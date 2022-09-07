package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.pcal.fastback.ModContext;

import java.nio.file.Path;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.tasks.ListSnapshotsTask.*;

public class ListCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, final ModContext ctx) {
        final ListCommand rc = new ListCommand(ctx);
        fastbackCmd.then(CommandManager.literal("list").executes(rc::execute));
    }

    private final ModContext ctx;

    private ListCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(final CommandContext<ServerCommandSource> cc) {
        final MinecraftServer server = cc.getSource().getServer();
        final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
        final Consumer<String> sink = message -> cc.getSource().sendFeedback(Text.literal(message), false);
        sink.accept("Local snapshots:");
        this.ctx.getExecutorService().execute(listSnapshotsForWorld(worldSaveDir, sink, ctx.getLogger()));
        return 1;
    }

}
