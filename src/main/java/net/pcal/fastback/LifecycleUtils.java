package net.pcal.fastback;

import net.minecraft.server.MinecraftServer;
import net.pcal.fastback.commands.Commands;
import net.pcal.fastback.logging.ChatLogger;
import net.pcal.fastback.logging.CompositeLogger;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.SaveScreenLogger;
import net.pcal.fastback.tasks.BackupTask;
import org.eclipse.jgit.api.Git;

import java.io.IOException;
import java.nio.file.Path;

import static net.minecraft.text.Text.translatable;
import static net.pcal.fastback.WorldConfig.isBackupsEnabledOn;
import static net.pcal.fastback.utils.GitUtils.isGitRepo;

public class LifecycleUtils {

    public static void onMinecraftStart(final ModContext ctx) {
        Commands.registerCommands(ctx, ctx.getCommandName());
        ctx.getLogger().info(ctx.getModId() + " initialized");
    }

    public static void onWorldStart(final ModContext ctx, final MinecraftServer server) {
        final Logger logger = ctx.isClient() ? CompositeLogger.of(ctx.getLogger(), new ChatLogger(ctx))
                : ctx.getLogger();
        final Path worldSaveDir = ctx.getWorldSaveDirectory(server);
        if (isGitRepo(worldSaveDir)) {
            try (final Git git = Git.open(worldSaveDir.toFile())) {
                WorldUtils.doWorldMaintenance(git, logger);
                if (WorldConfig.load(worldSaveDir).isBackupEnabled()) {
                    return;
                }
            } catch (IOException e) {
                logger.internalError("Unable to perform maintenance.  Backups will probably not work correctly", e);
            }
        }
        if (ctx.isStartupNotificationEnabled()) {
            logger.notify(translatable("fastback.notify.suggest-enable"));
        }
    }

    public static void onWorldStop(final ModContext ctx, final MinecraftServer server) {
        final Logger logger = ctx.isClient() ? CompositeLogger.of(ctx.getLogger(), new SaveScreenLogger(ctx))
                : ctx.getLogger();
        final Path worldSaveDir = ctx.getWorldSaveDirectory(server);
        if (!isBackupsEnabledOn(worldSaveDir)) {
            logger.notify(translatable("fastback.notify.suggest-enable"));
            return;
        }
        try {
            final WorldConfig config = WorldConfig.load(worldSaveDir);
            if (config.isShutdownBackupEnabled()) {
                final Logger screenLogger = CompositeLogger.of(ctx.getLogger(), new SaveScreenLogger(ctx));
                new BackupTask(worldSaveDir, screenLogger).run();
            } else {
                logger.info("Shutdown backups disabled.");
            }
        } catch (IOException e) {
            logger.internalError("Shutdown backup failed.", e);
        }
    }
}
