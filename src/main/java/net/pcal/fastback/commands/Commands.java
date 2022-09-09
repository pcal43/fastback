package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.tasks.TaskListener;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.nio.file.Path;

import static net.pcal.fastback.GitUtils.isGitRepo;
import static net.pcal.fastback.commands.CommandTaskListener.taskListener;

public class Commands {

    static int FAILURE = 0;
    static int SUCCESS = 1;

    public static void registerCommands(final ModContext ctx, final String fastbackCommand) {
        final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd = LiteralArgumentBuilder.literal(fastbackCommand);
        EnableCommand.register(fastbackCmd, ctx);
        DisableCommand.register(fastbackCmd, ctx);
        StatusCommand.register(fastbackCmd, ctx);
        RestoreCommand.register(fastbackCmd, ctx);
        ListCommand.register(fastbackCmd, ctx);
        RemoteCommand.register(fastbackCmd, ctx);
        CreateFileRemoteCommand.register(fastbackCmd, ctx);
        ShutdownCommand.register(fastbackCmd, ctx);
        UuidCommand.register(fastbackCmd, ctx);
        VersionCommand.register(fastbackCmd, ctx);
        NowCommand.register(fastbackCmd, ctx);
        CommandRegistrationCallback.EVENT.register((dispatcher, regAccess, env) -> dispatcher.register(fastbackCmd));
    }


    interface CommandLogic {
        int execute(StoredConfig gitConfig, WorldConfig worldConfig, TaskListener taskListener)
                throws IOException, GitAPIException;
    }

    static int executeStandard(final ModContext ctx, final CommandContext<ServerCommandSource> cc, CommandLogic sub) {
        final MinecraftServer server = cc.getSource().getServer();
        final TaskListener taskListener = taskListener(cc);
        final Path worldSaveDir = ctx.getWorldSaveDirectory(server);
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
            ctx.getLogger().error(e);
            return FAILURE;
        }
    }
}
