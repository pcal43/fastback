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
import net.minecraft.commands.CommandSourceStack;
import net.pcal.fastback.logging.UserLogger;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.UserLogger.ulog;
import static net.pcal.fastback.utils.Executor.ExecutionLock.NONE;

enum RemoteRestoreCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "remote-restore";
    private static final String ARGUMENT = "snapshot";

    @Override
    public void register(final LiteralArgumentBuilder<CommandSourceStack> argb, PermissionsFactory<CommandSourceStack> pf) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(COMMAND_NAME, pf)).then(
                                argument(ARGUMENT, StringArgumentType.string()).
                                        suggests(SnapshotNameSuggestions.remote()).
                                        executes(RemoteRestoreCommand::remoteRestore)
                        )
        );
    }

    private static int remoteRestore(final CommandContext<CommandSourceStack> cc) {
        final UserLogger ulog = ulog(cc);
        gitOp(NONE, ulog, repo -> {
            final String snapshotName = cc.getLastChild().getArgument(ARGUMENT, String.class);
            repo.doRestoreRemoteSnapshot(snapshotName, ulog);
        });
        return SUCCESS;
    }
}
