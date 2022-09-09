package net.pcal.gitback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.gitback.Loggr;
import net.pcal.gitback.ModContext;
import net.pcal.gitback.WorldConfig;
import net.pcal.gitback.tasks.TaskListener;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.gitback.WorldUtils.doWorldMaintenance;
import static net.pcal.gitback.commands.CommandTaskListener.taskListener;
import static net.pcal.gitback.commands.Commands.FAILURE;
import static net.pcal.gitback.commands.Commands.SUCCESS;

public class EnableCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final EnableCommand c = new EnableCommand(ctx);
        argb.then(
                literal("enable").executes(c::enable));
    }

    private final ModContext ctx;
    private final Loggr logger;

    private EnableCommand(ModContext context) {
        this.ctx = requireNonNull(context);
        this.logger = ctx.getLogger();
    }

    private int enable(final CommandContext<ServerCommandSource> cc) {
        final MinecraftServer server = cc.getSource().getServer();
        final TaskListener taskListener = taskListener(cc);
        final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
        try (final Git git = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
            doWorldMaintenance(git, logger);
            final StoredConfig config = git.getRepository().getConfig();
            final WorldConfig worldConfig = WorldConfig.load(worldSaveDir, config);
            if (worldConfig.isBackupEnabled() && worldConfig.isShutdownBackupEnabled()) {
                taskListener.error("Backups already enabled.");
                return FAILURE;
            } else {
                WorldConfig.setBackupEnabled(config, true);
                WorldConfig.setShutdownBackupEnabled(config, true);
                config.save();
                taskListener.feedback("Enabled automatic local backups on world shutdown.");
                return SUCCESS;
            }
        } catch (GitAPIException | IOException e) {
            taskListener.internalError();
            logger.error("Error enabling backups", e);
            return FAILURE;
        }
    }
}
