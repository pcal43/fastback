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
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.tasks.PruneTask;
import net.pcal.fastback.utils.SnapshotId;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.ModContext.ExecutionLock.WRITE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.Message.localized;

/**
 * Command to prune all snapshots that are not to be retained per the retention policy.
 *
 * @author pcal
 * @since 0.2.0
 */
enum PruneCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "prune";

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(cc -> prune(ctx, cc.getSource()))
        );
    }

    public static int prune(final ModContext ctx, final ServerCommandSource scs) {
        final Logger log = commandLogger(ctx, scs);
        gitOp(ctx, WRITE, log, git -> {
            final PruneTask pt = new PruneTask(git, ctx, log);
            final Collection<SnapshotId> pruned = pt.call();
            log.hud(null);
            log.chat(localized("fastback.chat.prune-done", pruned.size()));
            if (pruned.size() > 0) log.chat(localized("fastback.chat.prune-suggest-gc"));
        });
        return SUCCESS;
    }
}
