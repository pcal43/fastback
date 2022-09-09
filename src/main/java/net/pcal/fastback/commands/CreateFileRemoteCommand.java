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
import static net.pcal.fastback.FileUtils.mkdirs;
import static net.pcal.fastback.GitUtils.isGitRepo;
import static net.pcal.fastback.commands.CommandTaskListener.taskListener;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.executeStandard;

public class CreateFileRemoteCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, final ModContext ctx) {
        final CreateFileRemoteCommand c = new CreateFileRemoteCommand(ctx);
        fastbackCmd.then(
                literal("create-remote").then(
                        argument("file-path", StringArgumentType.greedyString()).
                                executes(c::setFileRemote))
        );
    }

    private final ModContext ctx;
    private final Loggr logger;

    private CreateFileRemoteCommand(ModContext context) {
        this.ctx = requireNonNull(context);
        this.logger = ctx.getLogger();
    }

    private int setFileRemote(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, tali) -> {
            final String targetPath = cc.getArgument("file-path", String.class);
            final Path fupHome = Path.of(targetPath);
            if (fupHome.toFile().exists()) {
                tali.error("Directory already exists:");
                tali.error(fupHome.toString());
                return FAILURE;
            }
            mkdirs(fupHome);
            try (Git targetGit = Git.init().setBare(wc.isFileRemoteBare()).setDirectory(fupHome.toFile()).call()) {
                final StoredConfig targetGitc = targetGit.getRepository().getConfig();
                targetGitc.setInt("core", null, "compression", 0);
                targetGitc.setInt("pack", null, "window", 0);
                targetGitc.save();
            }
            final String targetUrl = "file://" + fupHome.toAbsolutePath();
            WorldConfig.setRemoteUrl(gitc, targetUrl);
            WorldConfig.setRemoteBackupEnabled(gitc, true);
            gitc.save();
            tali.feedback("Git repository created at " + targetPath);
            tali.feedback("Remote backups enabled to:");
            tali.feedback(targetUrl);
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
