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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.config.GitConfigKey;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.mod.Mod;
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.repo.RepoFactory;

import java.nio.file.Path;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.missingArgument;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.config.FastbackConfigKey.IS_LOCK_CLEANUP_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.RESTORE_DIRECTORY;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.localized;
import static net.pcal.fastback.logging.UserMessage.raw;
import static net.pcal.fastback.mod.Mod.mod;

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
        registerBooleanConfigValue(IS_NATIVE_GIT_ENABLED, setCommand);
        registerBooleanConfigValue(IS_LOCK_CLEANUP_ENABLED, setCommand);
        registerStringConfigValue(RESTORE_DIRECTORY, setCommand);
        registerForceDebug(setCommand);
        root.then(setCommand);
    }


    // ======================================================================
    // Boolean config values

    private static void registerBooleanConfigValue(GitConfigKey key, final LiteralArgumentBuilder<ServerCommandSource> setCommand) {
        final LiteralArgumentBuilder<ServerCommandSource> builder = literal(key.getSettingDisplayName());
        builder.then(literal("true").executes(cc -> setBooleanConfigValue(cc, key, true)));
        builder.then(literal("false").executes(cc -> setBooleanConfigValue(cc, key, false)));
        setCommand.then(builder);
    }

    private static int setBooleanConfigValue(final CommandContext<ServerCommandSource> cc, GitConfigKey key, boolean value)  {
        try(UserLogger ulog = UserLogger.forCommand(cc)) {
            final Path worldSaveDir = mod().getWorldDirectory();
            final RepoFactory rf = RepoFactory.get();
            if (rf.isGitRepo(worldSaveDir)) {
                try (Repo repo = rf.load(worldSaveDir)) {
                    repo.setConfigValue(key, value, UserLogger.forCommand(cc));
                    ulog.message(raw(key.getSettingDisplayName() + " = " + value));
                } catch (Exception e) {
                    ulog.internalError(e);
                    return FAILURE;
                }
            }
        }
        return SUCCESS;
    }

    // ======================================================================
    // String config values

    private static final String STRING_VALUE = "value";

    private static void registerStringConfigValue(GitConfigKey key, final LiteralArgumentBuilder<ServerCommandSource> setCommand) {
        final LiteralArgumentBuilder<ServerCommandSource> builder = literal(key.getSettingDisplayName());
        builder.then(argument(STRING_VALUE, StringArgumentType.greedyString()).
                executes(cc -> setStringConfigValue(cc, key)));
        setCommand.then(builder);
    }

    private static int setStringConfigValue(final CommandContext<ServerCommandSource> cc, GitConfigKey key)  {
        try(UserLogger ulog = UserLogger.forCommand(cc)) {
            final Path worldSaveDir = mod().getWorldDirectory();
            final RepoFactory rf = RepoFactory.get();
            if (rf.isGitRepo(worldSaveDir)) {
                try (Repo repo = rf.load(worldSaveDir)) {
                    final String newValue = cc.getArgument(STRING_VALUE, String.class);
                    repo.getConfig().updater().set(key, newValue).save();
                    ulog.message(raw(key.getSettingDisplayName() + " = " + newValue));
                } catch (Exception e) {
                    ulog.internalError(e);
                    return FAILURE;
                }
            }
        }
        return SUCCESS;
    }


    // ======================================================================
    // force-debug

    private static void registerForceDebug(final LiteralArgumentBuilder<ServerCommandSource> setCommand) {
        final LiteralArgumentBuilder<ServerCommandSource> debug = literal("force-debug");
        debug.then(literal("enabled").executes(cc -> setForceDebug(cc, true)));
        debug.then(literal("disabled").executes(cc -> setForceDebug(cc, false)));
        setCommand.then(debug);
    }

    private static int setForceDebug(final CommandContext<ServerCommandSource> cc, boolean value) {
        syslog().setForceDebugEnabled(value);
        try(UserLogger ulog = UserLogger.forCommand(cc)) {
            ulog.message(localized("fastback.chat.ok"));
        }
        return SUCCESS;
    }
}
