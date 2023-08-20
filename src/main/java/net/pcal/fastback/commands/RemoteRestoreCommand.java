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
import net.pcal.fastback.ModContext;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.SnapshotId;

import java.nio.file.Path;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.ModContext.ExecutionLock.NONE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.config.GitConfigKey.REMOTE_PUSH_URL;
import static net.pcal.fastback.logging.Message.localized;

enum RemoteRestoreCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "remote-restore";
    private static final String ARGUMENT = "snapshot";


    @Override
    public void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).then(
                                argument(ARGUMENT, StringArgumentType.string()).
                                        suggests(SnapshotNameSuggestions.remote(ctx)).
                                        executes(cc -> remoteRestore(ctx, cc))
                        )
        );
    }

    private static int remoteRestore(final ModContext ctx, final CommandContext<ServerCommandSource> cc) {
        final Logger log = commandLogger(ctx, cc.getSource());
        gitOp(ctx, NONE, log, repo -> {
            final GitConfig conf = repo.getConfig();
            final String snapshotName = cc.getLastChild().getArgument(ARGUMENT, String.class);
            final SnapshotId sid = SnapshotId.fromUuidAndName(repo.getWorldUuid(), snapshotName);
            final String uri =  conf.getString(REMOTE_PUSH_URL);
            final Path restoreDir = repo.restoreSnapshotTask(uri, ctx.getRestoresDir(),
                    ctx.getWorldName(), sid, log).call();
            log.chat(localized("fastback.chat.restore-done", restoreDir));
        });
        return SUCCESS;
    }
}
