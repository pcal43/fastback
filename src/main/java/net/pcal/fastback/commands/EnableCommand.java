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

import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.CommandTaskListener.taskListener;

public class EnableCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, final ModContext ctx) {
        final EnableCommand c = new EnableCommand(ctx);
        fastbackCmd.then(
                literal("enable").executes(c::enableShutdown).then(
                        literal("remote").then(
                                argument("remote-url", StringArgumentType.string()).
                                        executes(c::enableRemoteUrl))).then(
                        literal("remote").executes(c::enableRemote)).then(
                        literal("shutdown").executes(c::enableShutdown)));
    }

    private final ModContext ctx;
    private final Loggr logger;

    private EnableCommand(ModContext context) {
        this.ctx = requireNonNull(context);
        this.logger = ctx.getLogger();
    }

    private int enableRemote(final CommandContext<ServerCommandSource> cc) {
        return internal_enableRemote(cc, null);
    }

    private int enableRemoteUrl(final CommandContext<ServerCommandSource> cc) {
        return internal_enableRemote(cc, requireNonNull(cc.getArgument("remote-url", String.class)));
    }

    private int internal_enableRemote(final CommandContext<ServerCommandSource> cc, String remoteUrl) {
        final MinecraftServer server = cc.getSource().getServer();
        final TaskListener taskListener = taskListener(cc);
        final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
        try (final Git git = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
            final StoredConfig gitConfig = git.getRepository().getConfig();
            if (remoteUrl == null) {
                final WorldConfig worldConfig = WorldConfig.load(worldSaveDir, gitConfig);
                remoteUrl = worldConfig.getRemotePushUri();
                if (remoteUrl == null) {
                    taskListener.error("Please specify a remote URL");
                    return FAILURE;
                }
            } else {
                WorldConfig.setRemoteUrl(gitConfig, remoteUrl);
            }
            WorldConfig.setBackupEnabled(gitConfig, true);
            WorldConfig.setRemoteBackupEnabled(gitConfig, true);
            gitConfig.save();
            taskListener.feedback("Enabled remote backups to " + remoteUrl);
            return SUCCESS;
        } catch (GitAPIException | IOException e) {
            taskListener.internalError();
            logger.error("Error enabling remote backups", e);
            return FAILURE;
        }
    }

    private int enableShutdown(final CommandContext<ServerCommandSource> cc) {
        final MinecraftServer server = cc.getSource().getServer();
        final TaskListener taskListener = taskListener(cc);
        final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
        try (final Git git = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
            final StoredConfig config = git.getRepository().getConfig();
            WorldConfig.setBackupEnabled(config, true);
            WorldConfig.setShutdownBackupEnabled(config, true);
            config.save();
            taskListener.feedback("Enabled automatic local backups on world shutdown.");
            return SUCCESS;
        } catch (GitAPIException | IOException e) {
            taskListener.internalError();
            logger.error("Error enabling backups", e);
            return FAILURE;
        }
    }
}
