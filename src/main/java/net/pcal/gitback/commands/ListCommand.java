package net.pcal.gitback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.pcal.gitback.ModContext;
import net.pcal.gitback.logging.Logger;

import java.nio.file.Path;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static net.pcal.gitback.GitUtils.isGitRepo;
import static net.pcal.gitback.commands.Commands.FAILURE;
import static net.pcal.gitback.commands.Commands.commandLogger;
import static net.pcal.gitback.tasks.ListSnapshotsTask.listSnapshotsForWorld;

public class ListCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final ListCommand rc = new ListCommand(ctx);
        argb.then(CommandManager.literal("list").executes(rc::execute));
    }

    private final ModContext ctx;

    private ListCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(final CommandContext<ServerCommandSource> cc) {
        final MinecraftServer server = cc.getSource().getServer();
        final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
        if (!isGitRepo(worldSaveDir)) {
            final Logger logger = commandLogger(ctx, cc);
            logger.notifyError("No backups available for this world.  Run '/backup enable' to enable backups.");
            return FAILURE;
        }
        final Consumer<String> sink = message -> cc.getSource().sendFeedback(Text.literal(message), false);
        sink.accept("Local snapshots:");
        this.ctx.getExecutorService().execute(listSnapshotsForWorld(worldSaveDir, sink, ctx.getLogger()));
        return 1;
    }

}
