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
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.mod.Mod;

import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.*;
import static net.pcal.fastback.commands.FullCommand.saveWorldBeforeBackup;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserLogger.ulog;
import static net.pcal.fastback.mod.Mod.mod;
import static net.pcal.fastback.repo.RepoFactory.rf;
import static net.pcal.fastback.utils.Executor.ExecutionLock.WRITE;

/**
 * Perform a local backup.
 *
 * @author pcal
 * @since 0.2.0
 */
enum LocalCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "local";

    @Override
    public void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final Mod mod) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(COMMAND_NAME)).
                        executes(cc -> run(mod, cc.getSource()))
        );
    }

    public static int run(Mod mod, ServerCommandSource scs) {
        try (final UserLogger ulog = ulog(scs)) {
            if (!rf().doInitCheck(mod().getWorldDirectory(), ulog)) return FAILURE;
            try {
                saveWorldBeforeBackup(mod, ulog);
            } catch (Exception e) {
                ulog.internalError();
                syslog().error(e);
            }
            gitOp(WRITE, ulog, repo -> repo.doCommitSnapshot(ulog));
        }
        return SUCCESS;
    }
}