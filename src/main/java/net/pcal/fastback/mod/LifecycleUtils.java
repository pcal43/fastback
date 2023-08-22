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
import net.pcal.fastback.logging.CompositeLogger;
import net.pcal.fastback.logging.ConsoleLogger;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.repo.RepoFactory;

import java.nio.file.Path;

import static net.pcal.fastback.config.GitConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.SHUTDOWN_ACTION;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.utils.EnvironmentUtils.getGitLfsVersion;
import static net.pcal.fastback.utils.EnvironmentUtils.getGitVersion;

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
        {
            final String gitVersion = getGitVersion();
            if (gitVersion == null) {
                syslog().warn("git is not installed.");
            } else {
                syslog().info("git is installed: " + gitVersion);
            }
        }
        {
            final String gitLfsVersion = getGitLfsVersion();
            if (gitLfsVersion == null) {
                syslog().warn("git-lfs is not installed.");
            } else {
                syslog().info("git-lfs is installed: " + gitLfsVersion);
            }
        }
        syslog().debug("onInitialize complete");

    }

    /**
     * Must be called when either client or server is terminating.
     */
    public static void onTermination(ModContext ctx) {
        syslog().debug("onTermination complete");
    }

    /**
     * Must be called when a world is starting (in either a dedicated or client-embedded server).
     */
    public static void onWorldStart(final ModContext ctx) {
        ctx.startExecutor();
        syslog().debug("onWorldStart complete");
    }

    /**
     * Must be called when a world is stopping (in either a dedicated or client-embedded server).
     */
    public static void onWorldStop(final ModContext mod) {
        final Logger consoleLogger = ConsoleLogger.get();
        final Logger logger = mod.isClient() ? CompositeLogger.of(consoleLogger) : consoleLogger;
        final Path worldSaveDir = mod.getWorldDirectory();
        logger.chat(UserMessage.localized("fastback.chat.thread-waiting"));
        mod.stopExecutor();
        final RepoFactory rf = RepoFactory.get();
        if (rf.isGitRepo(worldSaveDir)) {
            try (final Repo repo = rf.load(worldSaveDir, mod)) {
                final GitConfig config = repo.getConfig();
                if (config.getBoolean(IS_BACKUP_ENABLED)) {
                    final SchedulableAction action = SchedulableAction.forConfigValue(config, SHUTDOWN_ACTION);
                    if (action != null) {
                        final Logger screenLogger = CompositeLogger.of(consoleLogger); //FIXME figure out what to do with this
                        action.getTask(repo, logger).call();
                    }
                }
            } catch (Exception e) {
                syslog().error("Shutdown action failed.", e);
            }
        }
        syslog().debug("onWorldStop complete");
    }
}
