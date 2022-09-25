/*
 * FastBack - Fast, incremental Minecraft backups powered by Git.
 * Copyright (C) 2022 pcal.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */

package net.pcal.fastback;

import net.minecraft.server.MinecraftServer;
import net.pcal.fastback.commands.Commands;
import net.pcal.fastback.logging.ChatLogger;
import net.pcal.fastback.logging.CompositeLogger;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.SaveScreenLogger;
import net.pcal.fastback.tasks.BackupTask;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static net.pcal.fastback.WorldConfig.isBackupsEnabledOn;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.utils.FileUtils.writeResourceToFile;
import static net.pcal.fastback.utils.GitUtils.isGitRepo;

public class LifecycleUtils {

    public static void onClientStart(final ModContext ctx) {
        Commands.registerCommands(ctx, ctx.getCommandName());
        copyConfigResources(ctx);
        ctx.getLogger().info("onClientStart complete");
    }

    public static void onClientStop(ModContext ctx) {
        shutdownExecutor(ctx.getExecutorService());
        ctx.getLogger().info("onClientStop complete");
    }

    public static void onServerStart(final ModContext ctx) {
        Commands.registerCommands(ctx, ctx.getCommandName());
        copyConfigResources(ctx);
        ctx.getLogger().info("onServerStart complete");
    }

    public static void onServerStop(ModContext ctx) {
        shutdownExecutor(ctx.getExecutorService());
        ctx.getLogger().info("onServerStop complete");
    }

    public static void onWorldStart(final ModContext ctx, final MinecraftServer server) {
        final Logger logger = ctx.isClient() ? CompositeLogger.of(ctx.getLogger(), new ChatLogger(ctx))
                : ctx.getLogger();
        final Path worldSaveDir = ctx.getWorldSaveDirectory(server);
        if (isGitRepo(worldSaveDir)) {
            try (final Git git = Git.open(worldSaveDir.toFile())) {
                WorldConfig.doWorldMaintenance(git, logger);
            } catch (IOException e) {
                logger.internalError("Unable to perform maintenance.  Backups will probably not work correctly", e);
            }
        }
        ctx.getLogger().info("onWorldStart complete");
    }

    public static void onWorldStop(final ModContext ctx, final MinecraftServer server) {
        final Logger logger = ctx.isClient() ? CompositeLogger.of(ctx.getLogger(), new SaveScreenLogger(ctx))
                : ctx.getLogger();
        final Path worldSaveDir = ctx.getWorldSaveDirectory(server);
        if (!isBackupsEnabledOn(worldSaveDir)) {
            logger.notify(localized("fastback.notify.suggest-enable"));
            return;
        }
        try {
            final WorldConfig config = WorldConfig.load(worldSaveDir);
            if (config.isShutdownBackupEnabled()) {
                final Logger screenLogger = CompositeLogger.of(ctx.getLogger(), new SaveScreenLogger(ctx));
                new BackupTask(ctx, worldSaveDir, screenLogger).run();
            } else {
                logger.info("Shutdown backups disabled.");
            }
        } catch (IOException e) {
            logger.internalError("Shutdown backup failed.", e);
        }
        ctx.getLogger().info("onWorldStop complete");
    }

    private static final Iterable<Pair<String, Path>> CONFIG_RESOURCES = List.of(
            Pair.of("config/fastback/bin/enable", Path.of("bin/enable")),
            Pair.of("config/fastback/bin/git-hard-gc", Path.of("bin/git-hard-gc"))
    );

    private static void copyConfigResources(final ModContext ctx) {
        final Path configDir = ctx.getMinecraftConfigDir();
        for (final Pair<String, Path> pair : CONFIG_RESOURCES) {
            final String resourcePath = pair.getLeft();
            final Path targetFilePath = pair.getRight();
            ctx.getLogger().debug("writing "+resourcePath + " to "+targetFilePath);
            final Path configPath = configDir.resolve("fastback").resolve(targetFilePath);
            try {
                writeResourceToFile(resourcePath, configDir.resolve(configPath));
            } catch (IOException e) {
                ctx.getLogger().internalError("failed to output resource "+resourcePath, e);
            }
        }
    }

    /**
     * Lifted straight from the docs:
     * https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html
     */
    private static void shutdownExecutor(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(5, TimeUnit.MINUTES)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(5, TimeUnit.MINUTES))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

}
