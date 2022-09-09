package net.pcal.gitback;

import net.pcal.gitback.commands.Commands;
import net.pcal.gitback.tasks.BackupTask;
import net.pcal.gitback.tasks.TaskListener;

import java.io.IOException;
import java.nio.file.Path;

import static net.pcal.gitback.GitUtils.isGitRepo;

public class LifecycleUtils {

    public static void onMinecraftStart(final ModContext ctx) {
        Commands.registerCommands(ctx, ctx.getCommandName());
        ctx.getLogger().info(ctx.getModId() + " initialized");
    }

    public static void onWorldStart(final Path worldSaveDir, Loggr logger) {
        if (!isGitRepo(worldSaveDir)) {
            logger.info("Backups not enabled; to enable, run '/backup enable'");
            return;
        }
        try {
            WorldUtils.doWorldMaintenance(worldSaveDir, logger);
        } catch (IOException e) {
            logger.error("Unable to perform maintenance.  Backups will probably not work correctly", e);
        }
    }

    public static void onWorldStop(final Path worldSaveDir, Loggr logger) {
        if (!isGitRepo(worldSaveDir)) {
            logger.info("Backups not enabled; to enable, run '/backup enable'");
            return;
        }
        try {
            final WorldConfig config = WorldConfig.load(worldSaveDir);
            if (config.isShutdownBackupEnabled()) {
                new BackupTask(worldSaveDir, new TaskListener.NoListener(), logger).run();
            } else {
                logger.info("Shutdown backups disabled.");
            }
        } catch (IOException e) {
            logger.error("Shutdown backup failed.", e);
        }
    }

}
