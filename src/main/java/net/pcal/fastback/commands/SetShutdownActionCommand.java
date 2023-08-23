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
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mod.Mod;

import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.missingArgument;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.config.GitConfigKey.SHUTDOWN_ACTION;
import static net.pcal.fastback.utils.Executor.ExecutionLock.WRITE_CONFIG;

enum SetShutdownActionCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "set-shutdown-action";

    @Override
    public void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final Mod mod) {
        final LiteralArgumentBuilder<ServerCommandSource> setCommand = literal(COMMAND_NAME).
                requires(subcommandPermission(mod, COMMAND_NAME)).
                executes(cc -> missingArgument("actionName", mod, cc));
        for (final SchedulableAction action : SchedulableAction.values()) {
            final LiteralArgumentBuilder<ServerCommandSource> azz = literal(action.getArgumentName());
            azz.executes(cc -> setShutdownAction(mod, cc.getSource(), action));
            setCommand.then(azz);
        }
        argb.then(setCommand);
    }

    private static int setShutdownAction(final Mod mod, final ServerCommandSource scs, SchedulableAction action) {
        final UserLogger ulog = commandLogger(mod, scs);
        gitOp(mod, WRITE_CONFIG, ulog, repo -> {
            final GitConfig conf = repo.getConfig();
            conf.updater().set(SHUTDOWN_ACTION, action.getConfigValue()).save();
            ulog.message(UserMessage.localized("fastback.chat.info-shutdown-action", action.getArgumentName()));
        });
        return SUCCESS;
    }
}
