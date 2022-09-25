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
import net.pcal.fastback.ModContext;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.SnapshotId;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.executeStandard;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.logging.Message.raw;
import static net.pcal.fastback.tasks.ListSnapshotsTask.listSnapshotsForWorldSorted;
import static net.pcal.fastback.utils.GitUtils.isGitRepo;

public class ListCommand {

    private static final String COMMAND_NAME = "list";

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final ListCommand rc = new ListCommand(ctx);
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(rc::execute)
        );
    }

    private final ModContext ctx;

    private ListCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(ctx, cc, (gitc, wc, log) -> {
            final Path worldSaveDir = this.ctx.getWorldDirectory();
            if (!isGitRepo(worldSaveDir)) {
                final Logger logger = commandLogger(ctx, cc);
                logger.notifyError(localized("fastback.notify.not-enabled"));
                return FAILURE;
            }
            log.notify(localized("fastback.notify.list-local-snapshots-header"));
            this.ctx.execute(() -> {
                for (SnapshotId sid : listSnapshotsForWorldSorted(worldSaveDir, ctx.getLogger())) {
                    log.notify(raw(sid.getName()));
                }
            });
            return SUCCESS;
        });
    }
}
