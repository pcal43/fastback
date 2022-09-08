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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class DisableCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, final ModContext ctx) {
        final DisableCommand rc = new DisableCommand(ctx);
        fastbackCmd.then(CommandManager.literal("disable").executes(rc::execute));
    }

    private final ModContext ctx;

    private DisableCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(final CommandContext<ServerCommandSource> cc) {
        final MinecraftServer server = cc.getSource().getServer();
        final TaskListener taskListener = new ServerTaskListener(server.getCommandSource());
        final Loggr logger = this.ctx.getLogger();
        final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
        try (final Git git = Git.open(worldSaveDir.toFile())) {
            final StoredConfig config = git.getRepository().getConfig();
            WorldConfig.setBackupEnabled(config, false);
            WorldConfig.setShutdownBackupEnabled(config, false);
            config.save();
            taskListener.feedback("Backups disabled.");
            return 1;
        } catch (IOException e) {
            taskListener.internalError();
            logger.error("error enabling backups", e);
            return 0;
        }
    }
}
