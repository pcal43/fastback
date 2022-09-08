package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.Loggr;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.tasks.BackupTask;
import net.pcal.fastback.tasks.TaskListener;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.CommandTaskListener.taskListener;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;

public class NowCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, final ModContext ctx) {
        final NowCommand c = new NowCommand(ctx);
        fastbackCmd.then(literal("now").executes(c::now));
    }

    private final ModContext ctx;
    private final Loggr logger;

    private NowCommand(ModContext context) {
        this.ctx = requireNonNull(context);
        this.logger = ctx.getLogger();
    }

    private int now(CommandContext<ServerCommandSource> cc) {
        final MinecraftServer server = cc.getSource().getServer();
        final TaskListener taskListener = taskListener(cc);
        final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
        try {
            final WorldConfig config = WorldConfig.load(worldSaveDir);
            if (config.isBackupEnabled()) {
                new BackupTask(worldSaveDir, taskListener, logger).run();
            } else {
                taskListener.error("Backups are disabled.  Run '/backup enable' first.");
            }
            return SUCCESS;
        } catch (IOException e) {
            taskListener.internalError();
            logger.error("Shutdown backup failed.", e);
            return FAILURE;
        }
    }
}
