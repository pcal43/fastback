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

package net.pcal.fastback.mod;

import net.pcal.fastback.commands.Commands;
import net.pcal.fastback.commands.SchedulableAction;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.ChatLogger;
import net.pcal.fastback.logging.CompositeLogger;
import net.pcal.fastback.logging.ConsoleLogger;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.SaveScreenLogger;
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.repo.RepoFactory;

import java.nio.file.Path;

import static net.pcal.fastback.config.GitConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.SHUTDOWN_ACTION;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.utils.EnvironmentUtils.*;

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
        final Logger log = ConsoleLogger.get();
        {
            final String gitVersion = getGitVersion();
            if (gitVersion == null) {
                log.info("git is not installed.");
            } else {
                log.info("git is installed: " + gitVersion);
            }
        }
        {
            final String gitLfsVersion = getGitLfsVersion();
            if (gitLfsVersion == null) {
                log.info("git-lfs is not installed.");
            } else {
                log.info("git-lfs is installed: " + gitLfsVersion);
            }
        }
        log.info("onInitialize complete");

    }

    /**
     * Must be called when either client or server is terminating.
     */
    public static void onTermination(ModContext ctx) {
        ConsoleLogger.get().info("onTermination complete");
    }

    /**
     * Must be called when a world is starting (in either a dedicated or client-embedded server).
     */
    public static void onWorldStart(final ModContext ctx) {
        ctx.startExecutor();
        final Logger logger = ctx.isClient() ? CompositeLogger.of(ConsoleLogger.get(), new ChatLogger(ctx)) : ConsoleLogger.get(); //FIXME CAN WE KILL THIS?
        final Path worldSaveDir = ctx.getWorldDirectory();
        final RepoFactory rf = RepoFactory.get();
        if (rf.isGitRepo(worldSaveDir)) {
            try (Repo repo = rf.load(worldSaveDir, ctx, logger)) {
                repo.doWorldMaintenance(logger);
            } catch (Exception e) {
                logger.internalError("Unable to perform maintenance.  Backups will probably not work correctly", e);
            }
        }
        ConsoleLogger.get().info("onWorldStart complete");
    }

    /**
     * Must be called when a world is stopping (in either a dedicated or client-embedded server).
     */
    public static void onWorldStop(final ModContext mod) {
        final Logger consoleLogger = ConsoleLogger.get();
        final Logger logger = mod.isClient() ? CompositeLogger.of(consoleLogger, new SaveScreenLogger(mod))
                : consoleLogger;
        final Path worldSaveDir = mod.getWorldDirectory();
        logger.chat(localized("fastback.chat.thread-waiting"));
        mod.stopExecutor();
        final RepoFactory rf = RepoFactory.get();
        if (rf.isGitRepo(worldSaveDir)) {
            try (final Repo repo = rf.load(worldSaveDir, mod, logger)) {
                final GitConfig config = repo.getConfig();
                if (config.getBoolean(IS_BACKUP_ENABLED)) {
                    final SchedulableAction action = SchedulableAction.forConfigValue(config, SHUTDOWN_ACTION);
                    if (action != null) {
                        final Logger screenLogger = CompositeLogger.of(consoleLogger, new SaveScreenLogger(mod)); //FIXME figure out what to do with this
                        action.getTask(repo).call();
                    }
                }
            } catch (Exception e) {
                logger.internalError("Shutdown action failed.", e);
            }
        }
        consoleLogger.info("onWorldStop complete");
    }
}
