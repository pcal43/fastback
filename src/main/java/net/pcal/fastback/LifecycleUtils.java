package net.pcal.fastback;

import net.pcal.fastback.ModContext.WorldContext;
import net.pcal.fastback.commands.Commands;
import net.pcal.fastback.tasks.BackupTask;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Path;

import static net.pcal.fastback.LogUtils.error;
import static net.pcal.fastback.LogUtils.info;

public class LifecycleUtils {

    public static void onMinecraftStart(final ModContext mctx) {
        try {
            ModConfig.writeDefaultConfigFile();
            ModConfig.load(mctx.getLog4j());
        } catch (IOException e) {
            throw new RuntimeException("Configuration errors, cannot start", e);
        }
        Commands.registerCommands(mctx, "backup");
        mctx.getLogger().info("Fastback initialized");
    }

    public static void onWorldStart(final WorldContext world) {
        final Path worldSaveDir = world.getWorldSaveDirectory();
        final Logger logger = world.getModContext().getLog4j();
        final ModConfig modConfig;
        try {
            modConfig = ModConfig.loadForWorld(worldSaveDir, logger);
        } catch (IOException e) {
            error(logger, "Unable to load config, backups disabled", e);
            return;
        }
        if (!modConfig.getBoolean(ModConfig.Key.FASTBACK_ENABLED)) {
            info(logger, "Backups disabled in mod configuration.");
            return;
        }
        try {
            WorldUtils.doWorldMaintenance(modConfig, world, logger);
        } catch (IOException | GitAPIException e) {
            error(logger, "Unable to perform maintenance.  Backups will probably not work correctly", e);
        }

    }

    public static void onWorldStop(final WorldContext world) {
        final Logger logger = world.getModContext().getLog4j();
        final Path worldSaveDir = world.getWorldSaveDirectory();
        final ModConfig modConfig;
        try {
            modConfig = ModConfig.loadForWorld(worldSaveDir, logger);
        } catch (IOException e) {
            error(logger, "BACKUP FAILED.  Unable to load config.", e);
            return;
        }
        if (!modConfig.getBoolean(ModConfig.Key.FASTBACK_ENABLED)) {
            info(logger, "Backups disabled in mod configuration.");
            return;
        }
        if (modConfig.getBoolean(ModConfig.Key.SHUTDOWN_BACKUP_ENABLED)) {
            new BackupTask(modConfig, worldSaveDir, logger).run();
        } else {
            info(logger, "Shutdown backups disabled.");
        }
    }

}
