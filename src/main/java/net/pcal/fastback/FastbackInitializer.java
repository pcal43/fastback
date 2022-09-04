package net.pcal.fastback;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;

import static net.pcal.fastback.LogUtils.error;
import static net.pcal.fastback.LogUtils.info;

public class FastbackInitializer implements ModInitializer {

    private final Logger logger = LogManager.getLogger("Fastback");

    // ===================================================================================
    // ModInitializer implementation

    @Override
    public void onInitialize() {
        try {
            ModConfig.writeDefaultConfigFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            ModConfig.load(logger);
        } catch (IOException e) {
            throw new RuntimeException("Configuration errors, cannot start", e);
        }
        ServerLifecycleEvents.SERVER_STOPPED.register(this::onShutdown);
        ServerLifecycleEvents.SERVER_STARTING.register(this::onStartup);
        info(logger, "Fastback initialized");
    }

    private void onStartup(MinecraftServer server) {
        final ModConfig modConfig;
        try {
            modConfig = ModConfig.loadForWorld(server, logger);
        } catch (IOException e) {
            error(logger, "Unable to load config, backups disabled", e);
            return;
        }
        if (!modConfig.getBoolean(ModConfig.Key.FASTBACK_ENABLED)) {
            info(logger, "Backups disabled in mod configuration.");
            return;
        }
        try {
            WorldUtils.doWorldMaintenance(modConfig, server, logger);
        } catch (IOException | GitAPIException e) {
            error(logger, "Unable to perform maintenance.  Backups will probably not work correctly", e);
        }
    }

    private void onShutdown(MinecraftServer server) {
        final ModConfig modConfig;
        try {
            modConfig = ModConfig.loadForWorld(server, logger);
        } catch (IOException e) {
            error(logger, "BACKUP FAILED.  Unable to load config.", e);
            return;
        }
        if (!modConfig.getBoolean(ModConfig.Key.FASTBACK_ENABLED)) {
            info(logger, "Backups disabled in mod configuration.");
            return;
        }
        if (modConfig.getBoolean(ModConfig.Key.SHUTDOWN_BACKUP_ENABLED)) {
            new BackupTask(modConfig, server, this.logger).run();
        } else {
            info(logger, "Shutdown backups disabled.");
        }
    }
}
