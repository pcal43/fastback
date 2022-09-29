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

import net.pcal.fastback.commands.Commands;
import net.pcal.fastback.logging.ChatLogger;
import net.pcal.fastback.logging.CompositeLogger;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.SaveScreenLogger;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.utils.FileUtils.writeResourceToFile;
import static net.pcal.fastback.utils.GitUtils.isGitRepo;

/**
 * Framework-agnostic lifecycle logic.
 *
 * @author pcal
 * @since 0.0.1
 */
public class LifecycleUtils {

    /**
     * Must be called early in initialization of either a client or server.
     */
    public static void onInitialize(final ModContext ctx) {
        Commands.registerCommands(ctx, ctx.getCommandName());
        copyConfigResources(ctx);
        ctx.getLogger().info("onInitialize complete");
    }

    /**
     * Must be called when either client or server is terminating.
     */
    public static void onTermination(ModContext ctx) {
        ctx.getLogger().info("onTermination complete");
    }

    /**
     * Must be called when a world is starting (in either a dedicated or client-embedded server).
     */
    public static void onWorldStart(final ModContext ctx) {
        ctx.startExecutor();
        final Logger logger = ctx.isClient() ? CompositeLogger.of(ctx.getLogger(), new ChatLogger(ctx)) //FIXME CAN WE KILL THIS?
                : ctx.getLogger();
        final Path worldSaveDir = ctx.getWorldDirectory();
        if (isGitRepo(worldSaveDir)) {
            try (final Git git = Git.open(worldSaveDir.toFile())) {
                WorldConfig.doWorldMaintenance(git, logger);
            } catch (IOException e) {
                logger.internalError("Unable to perform maintenance.  Backups will probably not work correctly", e);
            }
        }
        ctx.getLogger().info("onWorldStart complete");
    }

    /**
     * Must be called when a world is stopping (in either a dedicated or client-embedded server).
     */
    public static void onWorldStop(final ModContext ctx) {
        final Logger logger = ctx.isClient() ? CompositeLogger.of(ctx.getLogger(), new SaveScreenLogger(ctx))
                : ctx.getLogger();
        final Path worldSaveDir = ctx.getWorldDirectory();
        logger.chat(localized("fastback.chat.thread-waiting"));
        ctx.stopExecutor();
        try (Git git = Git.open(worldSaveDir.toFile())) {
            final WorldConfig config = WorldConfig.load(git);
            if (config.isBackupEnabled() && config.shutdownAction() != null) {
                final Logger screenLogger = CompositeLogger.of(ctx.getLogger(), new SaveScreenLogger(ctx));
                config.shutdownAction().getRunnable(git, ctx, screenLogger).run();
            }
        } catch (IOException e) {
            logger.internalError("Shutdown action failed.", e);
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
            ctx.getLogger().debug("writing " + resourcePath + " to " + targetFilePath);
            final Path configPath = configDir.resolve("fastback").resolve(targetFilePath);
            try {
                writeResourceToFile(resourcePath, configDir.resolve(configPath));
            } catch (IOException e) {
                ctx.getLogger().internalError("failed to output resource " + resourcePath, e);
            }
        }
    }
}
