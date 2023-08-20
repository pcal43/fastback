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
import net.pcal.fastback.commands.SchedulableAction;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.ChatLogger;
import net.pcal.fastback.logging.CompositeLogger;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.SaveScreenLogger;
import net.pcal.fastback.repo.Repo;

import java.nio.file.Path;

import static net.pcal.fastback.config.GitConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.SHUTDOWN_ACTION;
import static net.pcal.fastback.logging.Message.localized;
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
            try (Repo repo = Repo.load(worldSaveDir, ctx, logger)) {
                repo.doWorldMaintenance(logger);
            } catch (Exception e) {
                logger.internalError("Unable to perform maintenance.  Backups will probably not work correctly", e);
            }
        }
        ctx.getLogger().info("onWorldStart complete");
    }

    /**
     * Must be called when a world is stopping (in either a dedicated or client-embedded server).
     */
    public static void onWorldStop(final ModContext mod) {
        final Logger logger = mod.isClient() ? CompositeLogger.of(mod.getLogger(), new SaveScreenLogger(mod))
                : mod.getLogger();
        final Path worldSaveDir = mod.getWorldDirectory();
        logger.chat(localized("fastback.chat.thread-waiting"));
        mod.stopExecutor();
        if (isGitRepo(worldSaveDir)) {
            try (final Repo repo = Repo.load(worldSaveDir, mod, logger)) {
                final GitConfig config = repo.getConfig();
                if (config.getBoolean(IS_BACKUP_ENABLED)) {
                    final SchedulableAction action = SchedulableAction.forConfigValue(config, SHUTDOWN_ACTION);
                    if (action != null) {
                        final Logger screenLogger = CompositeLogger.of(mod.getLogger(), new SaveScreenLogger(mod)); //FIXME figure out what to do with this
                        action.getTask(repo).call();
                    }
                }
            } catch (Exception e) {
                logger.internalError("Shutdown action failed.", e);
            }
        }
        mod.getLogger().info("onWorldStop complete");
    }

}
