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
import net.pcal.fastback.mod.Mod;

import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.missingArgument;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.localized;
import static net.pcal.fastback.utils.Executor.ExecutionLock.WRITE_CONFIG;

/**
 * Sets various configuration values.
 *
 * @author pcal
 * @since 0.13.0
 */
enum SetCommand implements Command {

    INSTANCE;

    // ======================================================================
    // Command implementation

    private static final String COMMAND_NAME = "set";

    @Override
    public void register(final LiteralArgumentBuilder<ServerCommandSource> root, final Mod mod) {
        final LiteralArgumentBuilder<ServerCommandSource> setCommand = literal(COMMAND_NAME).
                requires(subcommandPermission(mod, COMMAND_NAME)).
                executes(cc -> missingArgument("key", mod, cc));
        registerNativeGit(setCommand, mod);
        registerForceDebug(setCommand, mod);
        root.then(setCommand);
    }

    // ======================================================================
    // native-git

    private static void registerNativeGit(final LiteralArgumentBuilder<ServerCommandSource> setCommand, Mod mod) {
        final LiteralArgumentBuilder<ServerCommandSource> nativeGit = literal("native-git");
        nativeGit.then(literal("enabled").executes(cc -> setNativeGit(mod, cc, true)));
        nativeGit.then(literal("disabled").executes(cc -> setNativeGit(mod, cc, false)));
        setCommand.then(nativeGit);
    }

    private static int setNativeGit(final Mod mod, final CommandContext<ServerCommandSource> cc, boolean value) {
        final UserLogger ulog = commandLogger(mod, cc.getSource());
        gitOp(mod, WRITE_CONFIG, ulog, repo -> {
            repo.setNativeGitEnabled(value, ulog);
        });
        return SUCCESS;
    }

    // ======================================================================
    // force-debug

    private static void registerForceDebug(final LiteralArgumentBuilder<ServerCommandSource> setCommand, final Mod mod) {
        final LiteralArgumentBuilder<ServerCommandSource> debug = literal("force-debug");
        debug.then(literal("enabled").executes(cc -> setForceDebug(mod, cc, true)));
        debug.then(literal("disabled").executes(cc -> setForceDebug(mod, cc, false)));
        setCommand.then(debug);
    }

    private static int setForceDebug(final Mod mod, final CommandContext<ServerCommandSource> cc, boolean value) {
        syslog().setForceDebugEnabled(value);
        commandLogger(mod, cc.getSource()).message(localized("fastback.chat.ok"));
        return SUCCESS;
    }
}
