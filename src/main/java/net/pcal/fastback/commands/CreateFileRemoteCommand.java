package net.pcal.fastback.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.utils.FileUtils.mkdirs;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.executeStandard;

public class CreateFileRemoteCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final CreateFileRemoteCommand c = new CreateFileRemoteCommand(ctx);
        argb.then(
                literal("create-remote").then(
                        argument("file-path", StringArgumentType.greedyString()).
                                executes(c::setFileRemote))
        );
    }

    private final ModContext ctx;

    private CreateFileRemoteCommand(final ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int setFileRemote(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final String targetPath = cc.getArgument("file-path", String.class);
            final Path fupHome = Path.of(targetPath);
            if (fupHome.toFile().exists()) {
                log.notifyError("Directory already exists:");
                log.notifyError(fupHome.toString());
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
            log.notify("Git repository created at " + targetPath);
            log.notify("Remote backups enabled to:");
            log.notify(targetUrl);
            return SUCCESS;
        });
    }
}
