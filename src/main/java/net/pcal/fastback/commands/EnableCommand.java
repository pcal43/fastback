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
        final MinecraftServer server = cc.getSource().getServer();
        final TaskListener taskListener = new ServerTaskListener(server.getCommandSource());
        taskListener.feedback("Enabled remote backups");
        return 1;
    }
    private int enableRemoteUrl(final CommandContext<ServerCommandSource> cc) {
        String url = cc.getArgument("remote-url", String.class);
        final MinecraftServer server = cc.getSource().getServer();
        final TaskListener taskListener = new ServerTaskListener(server.getCommandSource());
        taskListener.feedback("Enabled remote backups to " + url);
        return 1;
    }

    private int enableShutdown(final CommandContext<ServerCommandSource> cc) {
        final MinecraftServer server = cc.getSource().getServer();
        final TaskListener taskListener = new ServerTaskListener(server.getCommandSource());
        final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
        try (final Git git = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
            final StoredConfig config = git.getRepository().getConfig();
            WorldConfig.setBackupEnabled(config, true);
            WorldConfig.setShutdownBackupEnabled(config, true);
            config.save();
            taskListener.feedback("Enabled automatic local backups on world shutdown.");
            return 1;
        } catch (GitAPIException | IOException e) {
            taskListener.internalError();
            logger.error("Error enabling backups", e);
            return 0;
        }
    }

}

/**
 * WorldConfig config = WorldConfig.load(worldSaveDir, git.getRepository().getConfig());
 * final URIish uri = new URIish(argument);
 * git.remoteSetUrl().setRemoteName(config.getRemoteName()).setRemoteUri(uri).call();
 * taskListener.feedback("Remote set.");
 **/
