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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mod.Mod;
import net.pcal.fastback.repo.SnapshotId;

import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.*;
import static net.pcal.fastback.logging.UserLogger.ulog;
import static net.pcal.fastback.utils.Executor.ExecutionLock.WRITE;

enum DeleteCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "delete";
    private static final String ARGUMENT = "snapshot";

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> argb, PermissionsFactory<ServerCommandSource> pf) {
        argb.then(literal(COMMAND_NAME).
                requires(subcommandPermission(COMMAND_NAME, pf)).then(
                        argument(ARGUMENT, StringArgumentType.string()).
                                suggests(SnapshotNameSuggestions.local()).
                                executes(DeleteCommand::delete)
                )
        );
    }

    private static int delete(final CommandContext<ServerCommandSource> cc) {
        final UserLogger log = ulog(cc);
        gitOp(WRITE, log, repo -> {
            final String snapshotName = getArgumentNicely(ARGUMENT, String.class, cc.getLastChild(), log);
            final SnapshotId sid = repo.createSnapshotId(snapshotName);
            final String branchName = sid.getBranchName();
            repo.deleteLocalBranches(List.of(branchName));
            log.message(UserMessage.localized("fastback.chat.delete-done", snapshotName));
        });
        return SUCCESS;
    }
}
