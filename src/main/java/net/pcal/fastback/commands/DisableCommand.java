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
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mod.Mod;

import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.config.GitConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.utils.Executor.ExecutionLock.WRITE_CONFIG;

enum DisableCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "disable";

    @Override
    public void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final Mod mod) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(mod, COMMAND_NAME)).
                        executes(cc -> disable(mod, cc))
        );
    }

    private static int disable(final Mod mod, final CommandContext<ServerCommandSource> cc) {
        final UserLogger ulog = commandLogger(mod, cc.getSource());
        gitOp(mod, WRITE_CONFIG, ulog, repo -> {
            repo.getConfig().updater().set(IS_BACKUP_ENABLED, false).save();
            ulog.chat(UserMessage.localized("fastback.chat.disable-done"));
        });
        return SUCCESS;
    }
}
