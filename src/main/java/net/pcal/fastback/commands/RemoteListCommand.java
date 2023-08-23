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

import com.google.common.collect.ListMultimap;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.config.GitConfigKey;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mod.Mod;
import net.pcal.fastback.repo.SnapshotId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.utils.Executor.ExecutionLock.NONE;

enum RemoteListCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "remote-list";

    @Override
    public void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final Mod mod) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(mod, COMMAND_NAME)).
                        executes(cc -> remoteList(mod, cc))
        );
    }

    private static int remoteList(final Mod mod, final CommandContext<ServerCommandSource> cc) {
        final UserLogger log = commandLogger(mod, cc.getSource());
        gitOp(mod, NONE, log, repo -> {
            final ListMultimap<String, SnapshotId> snapshotsPerWorld = repo.listRemoteSnapshots();
            final List<SnapshotId> snapshots = new ArrayList<>(snapshotsPerWorld.get(repo.getWorldUuid()));
            Collections.sort(snapshots);
            snapshots.forEach(sid -> log.chat(UserMessage.raw(sid.getName())));
            log.chat(UserMessage.localized("fastback.chat.remote-list-done", snapshots.size(), repo.getConfig().getString(GitConfigKey.REMOTE_PUSH_URL)));
            if (snapshotsPerWorld.keySet().size() > 1) {
                log.chat(UserMessage.localized("fastback.chat.remote-list-others",
                        snapshotsPerWorld.size() - 1, snapshotsPerWorld.size() - snapshots.size()));
            }
        });
        return SUCCESS;
    }
}
