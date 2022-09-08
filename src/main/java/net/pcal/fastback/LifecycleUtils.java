package net.pcal.fastback;

import net.pcal.fastback.commands.Commands;
import net.pcal.fastback.tasks.BackupTask;
import net.pcal.fastback.tasks.TaskListener;

import java.io.IOException;
import java.nio.file.Path;

public class LifecycleUtils {

    public static void onMinecraftStart(final ModContext mctx) {
        Commands.registerCommands(mctx, "backup");
        mctx.getLogger().info("Fastback initialized");
    }

    public static void onWorldStart(final Path worldSaveDir, Loggr logger) {
        try {
            WorldUtils.doWorldMaintenance(worldSaveDir, logger);
        } catch (IOException e) {
            logger.error("Unable to perform maintenance.  Backups will probably not work correctly", e);
        }
    }

    public static void onWorldStop(final Path worldSaveDir, Loggr logger) {
        try {
            final WorldConfig config = WorldConfig.load(worldSaveDir);
            if (config.isShutdownBackupEnabled()) {
                new BackupTask(worldSaveDir, new TaskListener.NoListener(), logger).run();
            } else {
                logger.info("Shutdown backups disabled.");
            }
        } catch(IOException e) {
            logger.error("Shutdown backup failed.", e);
        }
    }

}
