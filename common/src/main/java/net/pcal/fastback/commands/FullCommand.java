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
import net.minecraft.commands.CommandSourceStack;
import net.pcal.fastback.logging.UserLogger;

import java.io.IOException;

import static net.minecraft.commands.Commands.literal;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserLogger.ulog;
import static net.pcal.fastback.logging.UserMessage.localized;
import static net.pcal.fastback.mod.Mod.mod;
import static net.pcal.fastback.utils.Executor.ExecutionLock.WRITE;

/**
 * Perform a local backup.
 *
 * @author pcal
 * @since 0.2.0
 */
enum FullCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "full";

    public void register(final LiteralArgumentBuilder<CommandSourceStack> argb, PermissionsFactory<CommandSourceStack> pf) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(COMMAND_NAME, pf)).
                        executes(cc -> run(cc.getSource()))
        );
    }

    public static int run(CommandSourceStack scs) {
        final UserLogger ulog = ulog(scs);
        try {
            saveWorldBeforeBackup(ulog);
        } catch (IOException e) {
            ulog.internalError();
            syslog().error(e);
        }
        gitOp(WRITE, ulog, repo -> repo.doCommitAndPush(ulog));
        return SUCCESS;
    }

    /**
     * NOTE: this MUST be called in the game thread; calling it from one of our executor threads causes things
     * to seize up (at least on shutdown backup?)
     * <p>
     * Workaround for https://github.com/pcal43/fastback/issues/112
     */
    static void saveWorldBeforeBackup(UserLogger ulog) throws IOException {
        ulog.message(localized("fastback.chat.world-save"));
        mod().saveWorld();
        ulog.message(localized("fastback.message.backing-up"));
    }
}