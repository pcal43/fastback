package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.WorldUtils.doWorldMaintenance;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;

public class EnableCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final EnableCommand c = new EnableCommand(ctx);
        argb.then(
                literal("enable").executes(c::enable));
    }

    private final ModContext ctx;

    private EnableCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int enable(final CommandContext<ServerCommandSource> cc) {
        final MinecraftServer server = cc.getSource().getServer();
        final Logger logger = commandLogger(ctx, cc);
        final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
        try (final Git git = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
            doWorldMaintenance(git, logger);
            final StoredConfig config = git.getRepository().getConfig();
            final WorldConfig worldConfig = WorldConfig.load(worldSaveDir, config);
            if (worldConfig.isBackupEnabled() && worldConfig.isShutdownBackupEnabled()) {
                logger.notifyError("Backups already enabled.");
                return FAILURE;
            } else {
                WorldConfig.setBackupEnabled(config, true);
                WorldConfig.setShutdownBackupEnabled(config, true);
                config.save();
                logger.notify("Enabled automatic local backups on world shutdown.");
                return SUCCESS;
            }
        } catch (GitAPIException | IOException e) {
            logger.internalError("Error enabling backups", e);
            return FAILURE;
        }
    }
}
