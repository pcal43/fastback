package net.pcal.fastback.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.tasks.RestoreSnapshotTask;
import net.pcal.fastback.tasks.TaskListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class RestoreCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, ModContext ctx) {
        final RestoreCommand rc = new RestoreCommand(ctx);
        fastbackCmd.then(CommandManager.literal("restore").
                then(CommandManager.argument("snapshot", StringArgumentType.string()).
                        executes(rc::execute)));
    }

    private final ModContext ctx;

    private RestoreCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        final ModContext.WorldContext world = this.ctx.getWorldContext(cc.getSource().getServer());
        final Path worldSaveDir = world.getWorldSaveDirectory();
        final TaskListener tl = new ServerTaskListener(cc.getSource());
        final String snapshotName = cc.getArgument("snapshot", String.class);
        final Path targetDirectory;
        try {
            targetDirectory = Files.createTempDirectory("fastback-restore-");
        } catch (IOException e) {
            return 0;
        }
        this.ctx.getExecutorService().execute(
                RestoreSnapshotTask.create(worldSaveDir, snapshotName, world.getWorldName(),
                        targetDirectory, tl, this.ctx.getLogger()));
        return 1;
    }
}
