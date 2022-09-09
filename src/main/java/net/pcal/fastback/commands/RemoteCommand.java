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

public class RemoteCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, final ModContext ctx) {
        final RemoteCommand c = new RemoteCommand(ctx);
        fastbackCmd.then(
                literal("remote").executes(c::showRemote).then(
                        argument("remote-url", StringArgumentType.greedyString()).
                                executes(c::setRemoteUrl)).then(
                        literal("enable").executes(c::enable)).then(
                        literal("disable").executes(c::disable))
        );
    }

    private final ModContext ctx;
    private final Loggr logger;

    private RemoteCommand(ModContext context) {
        this.ctx = requireNonNull(context);
        this.logger = ctx.getLogger();
    }

    private int showRemote(final CommandContext<ServerCommandSource> cc) {
        return execute(cc, (gitConfig, worldConfig, tali) -> {
            final String remoteUrl = worldConfig.getRemotePushUri();
            final boolean enabled = worldConfig.isRemoteBackupEnabled();
            if (enabled && remoteUrl != null) {
                tali.feedback("Remote backups are enabled to:");
                tali.feedback(remoteUrl);
                return SUCCESS;
            } else {
                tali.error("Remote backups are disabled.");
                if (remoteUrl != null) {
                    tali.feedback("Run '/backup remote enable' to enable remote backups to:");
                    tali.feedback(remoteUrl);
                } else {
                    tali.feedback("Run '/backup remote <remote-url>' to enable remote backups.");
                }
                return FAILURE;
            }
        });
    }

    private int enable(final CommandContext<ServerCommandSource> cc) {
        return execute(cc, (gitConfig, worldConfig, taskListener) -> {
            final String newUrl = cc.getArgument("remote-url", String.class);
            final String currentUrl = worldConfig.getRemotePushUri();
            final boolean currentEnabled = worldConfig.isRemoteBackupEnabled();
            if (currentUrl == null) {
                taskListener.error("No remote URL is set.");
                taskListener.feedback("Run '/backup remote <remote-url>'");
                return FAILURE;
            } else if (currentEnabled) {
                taskListener.feedback("Remote backups are already enabled to:");
                taskListener.feedback(newUrl);
                return FAILURE;
            } else {
                WorldConfig.setRemoteBackupEnabled(gitConfig, false);
                gitConfig.save();
                return SUCCESS;
            }
        });
    }

    private int disable(final CommandContext<ServerCommandSource> cc) {
        return execute(cc, (gitConfig, worldConfig, taskListener) -> {
            final boolean currentEnabled = worldConfig.isRemoteBackupEnabled();
            if (!currentEnabled) {
                taskListener.error("Remote backups are already disabled.");
                return FAILURE;
            } else {
                WorldConfig.setRemoteBackupEnabled(gitConfig, false);
                gitConfig.save();
                taskListener.feedback("Remote backups disabled.");
                return SUCCESS;
            }
        });
    }

    private int setRemoteUrl(final CommandContext<ServerCommandSource> cc) {
        return execute(cc, (gitConfig, worldConfig, taskListener) -> {
            final String newUrl = cc.getArgument("remote-url", String.class);
            final String currentUrl = worldConfig.getRemotePushUri();
            final boolean currentEnable = worldConfig.isRemoteBackupEnabled();
            if (currentUrl != null && currentUrl.equals(newUrl)) {
                if (currentEnable) {
                    taskListener.feedback("Remote backups are already enabled to:");
                    taskListener.feedback(newUrl);
                    return SUCCESS;
                } else {
                    WorldConfig.setRemoteBackupEnabled(gitConfig, true);
                    gitConfig.save();
                    taskListener.feedback("Enabled remote backups to:");
                    taskListener.feedback(newUrl);
                }
            } else {
                WorldConfig.setRemoteUrl(gitConfig, newUrl);
                if (currentEnable) {
                    taskListener.feedback("Remote backup URL changed to " + newUrl);
                } else {
                    WorldConfig.setRemoteBackupEnabled(gitConfig, true);
                    taskListener.feedback("Enabled remote backups to:" + newUrl);
                    taskListener.feedback(newUrl);
                }
                gitConfig.save();
            }
            return SUCCESS;
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
