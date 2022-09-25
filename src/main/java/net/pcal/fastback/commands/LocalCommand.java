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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.tasks.CommitTask;

import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.executeStandardNew;
import static net.pcal.fastback.commands.Commands.subcommandPermission;

/**
 * Perform a local backup.
 *
 * @author pcal
 * @since 0.1.5
 */
public class LocalCommand {

    private static final String COMMAND_NAME = "local";

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(cc->run(ctx, cc.getSource()))
        );
    }

    public static int run(ModContext ctx, ServerCommandSource scs) throws CommandSyntaxException {
        return executeStandardNew(ctx, scs, (git, wc, log) -> {
            ctx.executeExclusive(() -> {
                new CommitTask(git, ctx, log).run();
            });
            return SUCCESS;
        });
    }
}
