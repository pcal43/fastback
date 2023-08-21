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
import net.pcal.fastback.mod.ModContext;
import net.pcal.fastback.logging.Logger;

import java.util.concurrent.Callable;

import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.mod.ModContext.ExecutionLock.WRITE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;


/**
 * Runs garbage collection to try to free up disk space.
 *
 * @author pcal
 * @since 0.0.12
 */
enum GcCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "gc";

    @Override
    public void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(cc -> gc(ctx, cc))
        );
    }

    private static int gc(ModContext ctx, CommandContext<ServerCommandSource> cc) {
        final Logger log = commandLogger(ctx, cc.getSource());
        gitOp(ctx, WRITE, log, repo -> {
            final Callable<Void> gc = repo.createGcTask();
            gc.call();
            //log.chat(localized("fastback.chat.gc-done", byteCountToDisplaySize(gc.getBytesReclaimed())));
        });
        return SUCCESS;
    }
}
