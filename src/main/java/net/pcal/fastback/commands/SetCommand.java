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
import static net.pcal.fastback.config.FastbackConfigKey.BROADCAST_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.BROADCAST_MESSAGE;
import static net.pcal.fastback.config.FastbackConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.IS_LOCK_CLEANUP_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.RESTORE_DIRECTORY;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserLogger.ulog;
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
        final LiteralArgumentBuilder<ServerCommandSource> sc = literal(COMMAND_NAME).
                requires(subcommandPermission(COMMAND_NAME)).
                executes(cc -> missingArgument("key", cc));
        registerBooleanConfigValue(IS_NATIVE_GIT_ENABLED, sc);
        registerBooleanConfigValue(IS_LOCK_CLEANUP_ENABLED, sc);
        registerBooleanConfigValue(IS_BACKUP_ENABLED, sc);
        registerBooleanConfigValue(BROADCAST_ENABLED, sc);
        registerStringConfigValue(BROADCAST_MESSAGE, "message", sc);
        registerStringConfigValue(RESTORE_DIRECTORY, "full-directory-path", sc);
        registerForceDebug(sc);
        root.then(sc);
    }


    // ======================================================================
    // Boolean config values

    private static void registerBooleanConfigValue(GitConfigKey key, final LiteralArgumentBuilder<ServerCommandSource> setCommand) {
        final LiteralArgumentBuilder<ServerCommandSource> builder = literal(key.getSettingDisplayName());
        builder.then(literal("true").executes(cc -> setBooleanConfigValue(key, true, cc)));
        builder.then(literal("false").executes(cc -> setBooleanConfigValue(key, false, cc)));
        setCommand.then(builder);
    }

    private static int setBooleanConfigValue(GitConfigKey key, boolean newValue, final CommandContext<ServerCommandSource> cc) {
        try (UserLogger ulog = ulog(cc)) {
            final Path worldSaveDir = mod().getWorldDirectory();
            final RepoFactory rf = RepoFactory.rf();
            if (rf.isGitRepo(worldSaveDir)) {
                try (Repo repo = rf.load(worldSaveDir)) {
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
    // String config values

    private static void registerStringConfigValue(GitConfigKey key, String argName, final LiteralArgumentBuilder<ServerCommandSource> setCommand) {
        final LiteralArgumentBuilder<ServerCommandSource> builder = literal(key.getSettingDisplayName());
        builder.then(argument(argName, StringArgumentType.greedyString()).
                executes(cc -> setStringConfigValue(key, argName, cc)));
        setCommand.then(builder);
    }

    private static int setStringConfigValue(GitConfigKey key, String argName, final CommandContext<ServerCommandSource> cc) {
        try (UserLogger ulog = ulog(cc)) {
            final Path worldSaveDir = mod().getWorldDirectory();
            final RepoFactory rf = RepoFactory.rf();
            if (rf.isGitRepo(worldSaveDir)) {
                try (Repo repo = rf.load(worldSaveDir)) {
                    final String newValue = cc.getArgument(argName, String.class);
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

    private static final String FORCE_DEBUG_SETTING = "force-debug-enabled";

    private static void registerForceDebug(final LiteralArgumentBuilder<ServerCommandSource> setCommand) {
        final LiteralArgumentBuilder<ServerCommandSource> debug = literal(FORCE_DEBUG_SETTING);
        debug.then(literal("true").executes(cc -> setForceDebug(cc, true)));
        debug.then(literal("false").executes(cc -> setForceDebug(cc, false)));
        setCommand.then(debug);
    }

    private static int setForceDebug(final CommandContext<ServerCommandSource> cc, boolean value) {
        syslog().setForceDebugEnabled(value);
        try (final UserLogger ulog = ulog(cc)) {
            ulog.message(raw("force-debug-enabled = " + value));
        }
        return SUCCESS;
    }
}
