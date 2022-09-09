package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.Loggr;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.tasks.TaskListener;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.GitUtils.isGitRepo;
import static net.pcal.fastback.WorldUtils.doWorldMaintenance;
import static net.pcal.fastback.commands.CommandTaskListener.taskListener;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;

public class EnableCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, final ModContext ctx) {
        final EnableCommand c = new EnableCommand(ctx);
        fastbackCmd.then(
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
            if (!isGitRepo(worldSaveDir)) {
                doWorldMaintenance(worldSaveDir, logger); // FIXME handoff of git instance could be cleaner
            }
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
