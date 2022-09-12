package net.pcal.fastback;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.pcal.fastback.commands.Commands;
import net.pcal.fastback.logging.CompositeLogger;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.tasks.BackupTask;
import org.eclipse.jgit.api.Git;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.GitUtils.isGitRepo;
import static net.pcal.fastback.WorldConfig.isBackupsEnabledOn;

public class LifecycleUtils {

    public static void onMinecraftStart(final ModContext ctx) {
        Commands.registerCommands(ctx, ctx.getCommandName());
        ctx.getLogger().info(ctx.getModId() + " initialized");
    }

    public static void onWorldStart(final ModContext ctx, final MinecraftServer server) {
        final Logger logger = ctx.getLogger();
        final Path worldSaveDir = ctx.getWorldSaveDirectory(server);
        if (isGitRepo(worldSaveDir)) {
            try (final Git git = Git.open(worldSaveDir.toFile())) {
                WorldUtils.doWorldMaintenance(git, logger);
            } catch (IOException e) {
                logger.internalError("Unable to perform maintenance.  Backups will probably not work correctly", e);
            }
        } else {
            logger.info("Backups not enabled; to enable, run '/backup enable'");
        }
    }

    public static void onWorldStop(final ModContext ctx, final MinecraftServer server) {
        final Logger logger = ctx.getLogger();
        final Path worldSaveDir = ctx.getWorldSaveDirectory(server);
        if (!isBackupsEnabledOn(worldSaveDir)) {
            logger.info("Backups not enabled; to enable, run '/backup enable'");
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

    private static class SaveScreenLogger implements Logger {

        private final ModContext ctx;

        SaveScreenLogger(ModContext ctx) {
            this.ctx = requireNonNull(ctx);
        }

        @Override
        public void progressComplete(String message, int percentage) {
            if (message.contains("Writing objects")) {
                message = "Uploading remote backup";
            } else if (message.contains("Finding sources")) {
                message = "Preparing remote backup";
            }
            this.ctx.setSavingScreenText(Text.literal(message + " " + percentage + "%"));
        }

        @Override
        public void progressComplete(String message) {
            if (message.contains("Writing objects")) {
                message = "Remote backup complete";
            }
            this.ctx.setSavingScreenText(Text.literal(message));
        }

        @Override
        public void notify(Text message) {
            this.ctx.setSavingScreenText(message);
        }

        @Override
        public void notifyError(Text message) {
            this.ctx.setSavingScreenText(message);
        }

        @Override
        public void internalError(String message, Throwable t) {
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void info(String message) {
        }

        @Override
        public void debug(String message) {
        }

        @Override
        public void debug(String message, Throwable t) {
        }
    }
}
