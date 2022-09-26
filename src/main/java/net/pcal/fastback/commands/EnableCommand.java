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

package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.WorldConfig.doWorldMaintenance;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.Message.localized;

public class EnableCommand {

    public static final SchedulableAction DEFAULT_SHUTDOWN_ACTION = SchedulableAction.FULL;

    private static final String COMMAND_NAME = "enable";

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final EnableCommand c = new EnableCommand(ctx);
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(c::enable)
        );
    }

    private final ModContext ctx;

    private EnableCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int enable(final CommandContext<ServerCommandSource> cc) {
        final Logger logger = commandLogger(ctx, cc);
        final Path worldSaveDir = this.ctx.getWorldDirectory();
        try (final Git git = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
            doWorldMaintenance(git, logger);
            final StoredConfig config = git.getRepository().getConfig();
            final WorldConfig worldConfig = WorldConfig.load(worldSaveDir, config);
            WorldConfig.setBackupEnabled(config, true);
            if (worldConfig.shutdownAction() == null) {
                WorldConfig.setShutdownAction(config, DEFAULT_SHUTDOWN_ACTION);
            }
            config.save();
            logger.notify(localized("fastback.notify.enable-done"));
            return SUCCESS;
        } catch (GitAPIException | IOException e) {
            logger.internalError("Error enabling backups", e);
            return FAILURE;
        }
    }
}
