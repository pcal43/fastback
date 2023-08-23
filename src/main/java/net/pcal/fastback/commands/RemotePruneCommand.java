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
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mod.Mod;
import net.pcal.fastback.repo.SnapshotId;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.utils.Executor.ExecutionLock.WRITE;

/**
 * Command to prune all snapshots that are not to be retained per the retention policy.
 *
 * @author pcal
 * @since 0.2.0
 */
enum RemotePruneCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "remote-prune";

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> argb, final Mod ctx) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(cc -> remotePrune(ctx, cc.getSource()))
        );
    }

    private static int remotePrune(final Mod ctx, final ServerCommandSource scs) {
        final UserLogger ulog = commandLogger(ctx, scs);
        gitOp(ctx, WRITE, ulog, repo -> {
            final Collection<SnapshotId> pruned = repo.doRemotePrune(ulog);
            ulog.chat(UserMessage.localized("fastback.chat.prune-done", pruned.size()));
        });
        return SUCCESS;
    }
}
