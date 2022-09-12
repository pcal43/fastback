package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.ChatLogger;
import net.pcal.fastback.logging.CompositeLogger;
import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.nio.file.Path;

import static net.pcal.fastback.GitUtils.isGitRepo;

public class Commands {

    static int FAILURE = 0;
    static int SUCCESS = 1;

    public static void registerCommands(final ModContext ctx, final String cmd) {
        final LiteralArgumentBuilder<ServerCommandSource> argb = LiteralArgumentBuilder.literal(cmd);
        EnableCommand.register(argb, ctx);
        DisableCommand.register(argb, ctx);
        StatusCommand.register(argb, ctx);
        RestoreCommand.register(argb, ctx);
        PurgeCommand.register(argb, ctx);
        ListCommand.register(argb, ctx);
        RemoteCommand.register(argb, ctx);
        CreateFileRemoteCommand.register(argb, ctx);
        ShutdownCommand.register(argb, ctx);
        UuidCommand.register(argb, ctx);
        VersionCommand.register(argb, ctx);
        HelpCommand.register(argb, ctx);
        if (ctx.isUnsafeCommandsEnabled()) {
            NowCommand.register(argb, ctx);
            GcCommand.register(argb, ctx);
            GcInfoCommand.register(argb, ctx);
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, regAccess, env) -> dispatcher.register(argb));
    }

    public static Logger commandLogger(final ModContext ctx, final CommandContext<ServerCommandSource> cc) {
        return CompositeLogger.of(
                ctx.getLogger(),
                new ChatLogger(cc.getSource())
        );
    }

    interface CommandLogic {
        int execute(StoredConfig gitConfig, WorldConfig worldConfig, Logger logger)
                throws IOException, GitAPIException;
    }

    static int executeStandard(final ModContext ctx, final CommandContext<ServerCommandSource> cc, CommandLogic sub) {
        final MinecraftServer server = cc.getSource().getServer();
        final Logger logger = commandLogger(ctx, cc);
        final Path worldSaveDir = ctx.getWorldSaveDirectory(server);
        if (!isGitRepo(worldSaveDir)) {
            logger.notifyError("Backups are not enabled on this world.  Run '/backup enable'");
            return FAILURE;
        }
        try (final Git git = Git.open(worldSaveDir.toFile())) {
            final StoredConfig gitConfig = git.getRepository().getConfig();
            final WorldConfig worldConfig = WorldConfig.load(worldSaveDir, gitConfig);
            if (!worldConfig.isBackupEnabled()) {
                logger.notifyError("Backups are not enabled on this world.  Run '/backup enable'");
                return FAILURE;
            }
            return sub.execute(gitConfig, worldConfig, logger);
        } catch (Exception e) {
            logger.internalError("Command execution failed.", e);
            return FAILURE;
        }
    }
}
