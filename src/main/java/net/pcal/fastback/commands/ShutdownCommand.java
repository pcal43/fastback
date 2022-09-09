package net.pcal.fastback.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
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
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.GitUtils.isGitRepo;
import static net.pcal.fastback.commands.CommandTaskListener.taskListener;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;

public class ShutdownCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, final ModContext ctx) {
        final ShutdownCommand c = new ShutdownCommand(ctx);
        fastbackCmd.then(
                literal("shutdown").executes(c::show).then(
                        literal("enable").executes(c::enable)).then(
                        literal("disable").executes(c::disable))
        );
    }

    private final ModContext ctx;
    private final Loggr logger;

    private ShutdownCommand(ModContext context) {
        this.ctx = requireNonNull(context);
        this.logger = ctx.getLogger();
    }

    private int show(final CommandContext<ServerCommandSource> cc) {
        return execute(cc, (gitConfig, worldConfig, tali) -> {
            final boolean enabled = worldConfig.isShutdownBackupEnabled();
            if (enabled) {
                tali.feedback("Backup on shutdown is currently enabled.");
            } else {
                tali.feedback("Backup on shutdown is currently disabled.");
            }
            return SUCCESS;
        });
    }

    private int enable(final CommandContext<ServerCommandSource> cc) {
        return execute(cc, (gitConfig, worldConfig, tali) -> {
            final boolean enabled = worldConfig.isShutdownBackupEnabled();
            if (enabled) {
                tali.error("Backup on world shutdown is already enabled.");
                return FAILURE;
            } else {
                WorldConfig.setShutdownBackupEnabled(gitConfig, true);
                gitConfig.save();
                tali.feedback("Backup on world shutdown enabled.");
                return SUCCESS;
            }
        });
    }


    private int disable(final CommandContext<ServerCommandSource> cc) {
        return execute(cc, (gitConfig, worldConfig, tali) -> {
            final boolean enabled = worldConfig.isShutdownBackupEnabled();
            if (!enabled) {
                tali.error("Backup on shutdown is already disabled.");
                return FAILURE;
            } else {
                WorldConfig.setShutdownBackupEnabled(gitConfig, true);
                gitConfig.save();
                tali.feedback("Backup on world shutdown disabled.");
                return SUCCESS;
            }
        });
    }

    private interface RemoteWork {
        int execute(StoredConfig gitConfig, WorldConfig worldConfig, TaskListener taskListener) throws IOException, GitAPIException;
    }

    private int execute(final CommandContext<ServerCommandSource> cc, RemoteWork sub) {
        final MinecraftServer server = cc.getSource().getServer();
        final TaskListener taskListener = taskListener(cc);
        final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
        if (!isGitRepo(worldSaveDir)) {
            taskListener.backupsNotEnabled();
            return FAILURE;
        }
        try (final Git git = Git.open(worldSaveDir.toFile())) {
            final StoredConfig gitConfig = git.getRepository().getConfig();
            final WorldConfig worldConfig = WorldConfig.load(worldSaveDir, gitConfig);
            if (!worldConfig.isBackupEnabled()) {
                taskListener.backupsNotEnabled();
                return FAILURE;
            }
            return sub.execute(gitConfig, worldConfig, taskListener);
        } catch (Exception e) {
            taskListener.internalError();
            logger.error(e);
            return FAILURE;
        }
    }
}
