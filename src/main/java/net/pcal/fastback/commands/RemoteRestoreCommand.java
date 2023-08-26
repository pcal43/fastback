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
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mod.Mod;
import net.pcal.fastback.repo.SnapshotId;

import java.nio.file.Path;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.config.OtherConfigKey.REMOTE_PUSH_URL;
import static net.pcal.fastback.utils.Executor.ExecutionLock.NONE;

enum RemoteRestoreCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "remote-restore";
    private static final String ARGUMENT = "snapshot";


    @Override
    public void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final Mod mod) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(mod, COMMAND_NAME)).then(
                                argument(ARGUMENT, StringArgumentType.string()).
                                        suggests(SnapshotNameSuggestions.remote()).
                                        executes(cc -> remoteRestore(mod, cc))
                        )
        );
    }

    private static int remoteRestore(final Mod mod, final CommandContext<ServerCommandSource> cc) {
        final UserLogger log = commandLogger(mod, cc.getSource());
        gitOp(mod, NONE, log, repo -> {
            final GitConfig conf = repo.getConfig();
            final String snapshotName = cc.getLastChild().getArgument(ARGUMENT, String.class);
            final SnapshotId sid = repo.createSnapshotId(snapshotName);
            final String uri = conf.getString(REMOTE_PUSH_URL);
            final Path restoreDir = repo.doRestoreSnapshot(uri, mod.getDefaultRestoresDir(), mod.getWorldName(), sid, log);
            log.message(UserMessage.localized("fastback.chat.restore-done", restoreDir));
        });
        return SUCCESS;
    }
}
