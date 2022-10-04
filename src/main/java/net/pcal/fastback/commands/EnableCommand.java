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

import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.ModContext.ExecutionLock.NONE;
import static net.pcal.fastback.WorldConfig.doWorldMaintenance;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.Message.localized;

enum EnableCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "enable";

    @Override
    public void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(cc -> enable(ctx, cc))
        );
    }

    private static int enable(final ModContext ctx, final CommandContext<ServerCommandSource> cc) {
        final Logger log = commandLogger(ctx, cc.getSource());
        ctx.execute(NONE, log, () -> {
                    final Path worldSaveDir = ctx.getWorldDirectory();
                    try (final Git git = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
                        doWorldMaintenance(git, log);
                        final StoredConfig config = git.getRepository().getConfig();
                        final WorldConfig worldConfig = WorldConfig.load(git);
                        WorldConfig.setBackupEnabled(config, true);
                        if (worldConfig.shutdownAction() == null) {
                            WorldConfig.setShutdownAction(config, SchedulableAction.DEFAULT_SHUTDOWN_ACTION);
                        }
                        config.save();
                        log.chat(localized("fastback.chat.enable-done"));
                    } catch (GitAPIException | IOException e) {
                        log.internalError("Error enabling backups", e);
                    }
                }
        );
        return SUCCESS;
    }
}
