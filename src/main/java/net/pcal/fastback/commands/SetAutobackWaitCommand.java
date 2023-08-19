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

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.config.GitConfigKey;
import net.pcal.fastback.logging.Logger;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.ModContext.ExecutionLock.WRITE_CONFIG;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.Message.localized;

enum SetAutobackWaitCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "set-autoback-wait";
    private static final String ARGUMENT = "wait";

    @Override
    public void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).then(
                                argument(ARGUMENT, IntegerArgumentType.integer(0)).
                                        executes(cc -> setWait(ctx, cc))
                        )
        );
    }

    private static int setWait(final ModContext ctx, final CommandContext<ServerCommandSource> cc) {
        final Logger log = commandLogger(ctx, cc.getSource());
        gitOp(ctx, WRITE_CONFIG, log, jgit -> {
            final int waitMinutes = cc.getArgument(ARGUMENT, int.class);
            final GitConfig conf = GitConfig.load(jgit);
            conf.updater().set(GitConfigKey.AUTOBACK_WAIT_MINUTES, waitMinutes).save();
            log.chat(localized("fastback.chat.info-autoback-wait", waitMinutes));
        });
        return SUCCESS;
    }
}
