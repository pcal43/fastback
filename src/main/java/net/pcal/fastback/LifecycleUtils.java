package net.pcal.fastback;

import net.pcal.fastback.commands.Commands;
import net.pcal.fastback.tasks.BackupTask;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;

public class LifecycleUtils {

    public static void onMinecraftStart(final ModContext mctx) {
        try {
            ModConfig.writeDefaultConfigFile();
            ModConfig.load(mctx.getLogger());
        } catch (IOException e) {
            throw new RuntimeException("Configuration errors, cannot start", e);
        }
        Commands.registerCommands(mctx, "backup");
        mctx.getLogger().info("Fastback initialized");
    }

    public static void onWorldStart(final Path worldSaveDir, Loggr logger) {
        final ModConfig modConfig;
        try {
            modConfig = ModConfig.loadForWorld(worldSaveDir, logger);
        } catch (IOException e) {
            logger.error("Unable to load config, backups disabled", e);
            return;
        }
        if (!modConfig.getBoolean(ModConfig.Key.FASTBACK_ENABLED)) {
            logger.info("Backups disabled in mod configuration.");
            return;
        }
        try {
            WorldUtils.doWorldMaintenance(modConfig, worldSaveDir, logger);
        } catch (IOException | GitAPIException e) {
            logger.error("Unable to perform maintenance.  Backups will probably not work correctly", e);
        }

    }

    public static void onWorldStop(final Path worldSaveDir, Loggr logger) {
        final ModConfig modConfig;
        try {
            modConfig = ModConfig.loadForWorld(worldSaveDir, logger);
        } catch (IOException e) {
            logger.error("BACKUP FAILED.  Unable to load config.", e);
            return;
        }
        if (!modConfig.getBoolean(ModConfig.Key.FASTBACK_ENABLED)) {
            logger.info("Backups disabled in mod configuration.");
            return;
        }
        if (modConfig.getBoolean(ModConfig.Key.SHUTDOWN_BACKUP_ENABLED)) {
            new BackupTask(modConfig, worldSaveDir, logger).run();
        } else {
            logger.info("Shutdown backups disabled.");
        }
    }

}
