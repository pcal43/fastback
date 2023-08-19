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
import net.pcal.fastback.ModContext;
import net.pcal.fastback.repo.RepoConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.tasks.ListSnapshotsTask;
import net.pcal.fastback.utils.SnapshotId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.ModContext.ExecutionLock.NONE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.logging.Message.raw;

enum RemoteListCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "remote-list";

    @Override
    public void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(cc -> remoteList(ctx, cc))
        );
    }

    private static int remoteList(final ModContext ctx, final CommandContext<ServerCommandSource> cc) {
        final Logger log = commandLogger(ctx, cc.getSource());
        gitOp(ctx, NONE, log, git -> {
            final RepoConfig wc = RepoConfig.load(git);
            final ListMultimap<String, SnapshotId> snapshotsPerWorld = ListSnapshotsTask.listRemoteSnapshots(git, wc, log);
            final List<SnapshotId> snapshots = new ArrayList<>(snapshotsPerWorld.get(wc.worldUuid()));
            Collections.sort(snapshots);
            snapshots.forEach(sid -> log.chat(raw(sid.getName())));
            log.chat(localized("fastback.chat.remote-list-done", snapshots.size(), wc.getRemotePushUrl()));
            if (snapshotsPerWorld.keySet().size() > 1) {
                log.chat(localized("fastback.chat.remote-list-others",
                        snapshotsPerWorld.size() - 1, snapshotsPerWorld.size() - snapshots.size()));
            }
        });
        return SUCCESS;
    }
}
