package net.pcal.gitback.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.gitback.ModContext;
import net.pcal.gitback.tasks.RestoreSnapshotTask;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.pcal.gitback.commands.CommandTaskListener.taskListener;
import static net.pcal.gitback.commands.Commands.SUCCESS;
import static net.pcal.gitback.commands.Commands.executeStandard;

public class RestoreCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> argb, ModContext ctx) {
        final RestoreCommand rc = new RestoreCommand(ctx);
        argb.then(CommandManager.literal("restore").
                then(CommandManager.argument("snapshot", StringArgumentType.string()).
                        executes(rc::execute)));
    }

    private final ModContext ctx;

    private RestoreCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, tali) -> {
            final String snapshotName = cc.getArgument("snapshot", String.class);
            final Path restoresDir = this.ctx.getRestoresDir();
            final MinecraftServer server = cc.getSource().getServer();
            final String worldName = this.ctx.getWorldName(server);
            final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
            this.ctx.getExecutorService().execute(
                    RestoreSnapshotTask.create(worldSaveDir, snapshotName, worldName,
                            restoresDir, taskListener(cc), this.ctx.getLogger()));
            return SUCCESS;
        });
    }
}
