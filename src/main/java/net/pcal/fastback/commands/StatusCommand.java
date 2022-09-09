package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.Loggr;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.tasks.TaskListener;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.GitUtils.isGitRepo;
import static net.pcal.fastback.commands.CommandTaskListener.taskListener;
import static net.pcal.fastback.commands.Commands.FAILURE;

public class StatusCommand {

    public static void register(LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, ModContext ctx) {
        final StatusCommand rc = new StatusCommand(ctx);
        fastbackCmd.then(CommandManager.literal("status").executes(rc::execute));
    }

    private final ModContext ctx;
    private final Loggr logger;

    private StatusCommand(ModContext context) {
        this.ctx = requireNonNull(context);
        this.logger = requireNonNull(ctx.getLogger());
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        final MinecraftServer server = cc.getSource().getServer();
        final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
        final TaskListener tl = taskListener(cc);
        if (!isGitRepo(worldSaveDir)) {
            tl.error("Run '/backup enable' to enable backups.");
            return FAILURE;
        }
        final WorldConfig config;
        try {
            config = WorldConfig.load(worldSaveDir);
        } catch (IOException ex) {
            tl.internalError();
            logger.error(ex);
            return 0;
        }
        tl.feedback("Local backup:  " + (config.isBackupEnabled() ? "enabled" : "disabled"));
        tl.feedback("Remote backup: " + (config.isRemoteBackupEnabled() ? "enabled" : "disabled"));
        if (config.isRemoteBackupEnabled()) {
            String url = config.getRemotePushUrl();
            if (url == null) {
                tl.error("Remote URL: Not Configured.  Run /backup remote [url]");
            } else {
                tl.feedback("Remote URL: " + url);
            }
        }
        tl.feedback("On shutdown:  " + (config.isShutdownBackupEnabled() ? "enabled" : "disabled"));
        return 1;
    }
}
