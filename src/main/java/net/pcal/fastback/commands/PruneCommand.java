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
enum PruneCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "prune";

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> argb, final Mod mod) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(mod, COMMAND_NAME)).
                        executes(cc -> prune(mod, cc.getSource()))
        );
    }

    private static int prune(final Mod mod, final ServerCommandSource scs) {
        final UserLogger ulog = commandLogger(mod, scs);
        gitOp(mod, WRITE, ulog, repo -> {
            final Collection<SnapshotId> pruned = repo.doLocalPrune(ulog);
            ulog.chat(UserMessage.localized("fastback.chat.prune-done", pruned.size()));
            if (pruned.size() > 0) ulog.chat(UserMessage.localized("fastback.chat.prune-suggest-gc"));
        });
        return SUCCESS;
    }
}
