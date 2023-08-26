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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.config.GitConfigKey;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.mod.Mod;
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.repo.RepoFactory;
import net.pcal.fastback.retention.RetentionPolicyType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.missingArgument;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.config.FastbackConfigKey.AUTOBACK_ACTION;
import static net.pcal.fastback.config.FastbackConfigKey.AUTOBACK_WAIT_MINUTES;
import static net.pcal.fastback.config.FastbackConfigKey.BROADCAST_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.BROADCAST_MESSAGE;
import static net.pcal.fastback.config.FastbackConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.IS_LOCK_CLEANUP_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.REMOTE_RETENTION_POLICY;
import static net.pcal.fastback.config.FastbackConfigKey.RESTORE_DIRECTORY;
import static net.pcal.fastback.config.FastbackConfigKey.SHUTDOWN_ACTION;
import static net.pcal.fastback.config.OtherConfigKey.REMOTE_PUSH_URL;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserLogger.ulog;
import static net.pcal.fastback.logging.UserMessage.raw;
import static net.pcal.fastback.mod.Mod.mod;
import static net.pcal.fastback.repo.RepoFactory.rf;

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
        registerBooleanConfigValue(IS_NATIVE_GIT_ENABLED, null, sc);
        registerBooleanConfigValue(IS_LOCK_CLEANUP_ENABLED, null, sc);
        registerBooleanConfigValue(IS_BACKUP_ENABLED, null, sc);
        registerBooleanConfigValue(BROADCAST_ENABLED, null, sc);
        registerStringConfigValue(BROADCAST_MESSAGE, null, "message", sc);
        registerStringConfigValue(RESTORE_DIRECTORY, null, "full-directory-path", sc);
        registerStringConfigValue(REMOTE_PUSH_URL, "remote-url", "url", sc);
        registerIntegerConfigValue(AUTOBACK_WAIT_MINUTES, null, "minutes", sc);

        {
            final List<String> schedulableActions = new ArrayList<>();
            for (final SchedulableAction sa : SchedulableAction.values()) {
                schedulableActions.add(sa.getConfigValue());
            }
            registerSelectConfigValue(AUTOBACK_ACTION, null, schedulableActions, sc);
            registerSelectConfigValue(SHUTDOWN_ACTION, null, schedulableActions, sc);
        }
        {
            final List<String> retentionPolicies = new ArrayList<>();
            for (final RetentionPolicyType rpt : RetentionPolicyType.getAvailable()) {
                retentionPolicies.add(rpt.getConfigValue());
            }
            registerSelectConfigValue(AUTOBACK_ACTION, null, schedulableActions, sc);
            registerSelectConfigValue(SHUTDOWN_ACTION, null, schedulableActions, sc);
        }
        registerForceDebug(sc);
        root.then(sc);
    }


    // ======================================================================
    // Boolean config values

    private static void registerBooleanConfigValue(final GitConfigKey key, final String keyDisplayOrNull, final LiteralArgumentBuilder<ServerCommandSource> setCommand) {
        final String keyDisplay = keyDisplayOrNull != null ? keyDisplayOrNull : key.getSettingName();
        final LiteralArgumentBuilder<ServerCommandSource> builder = literal(keyDisplay);
        builder.then(literal("true").executes(cc -> setBooleanConfigValue(key, keyDisplay, true, cc)));
        builder.then(literal("false").executes(cc -> setBooleanConfigValue(key, keyDisplay, false, cc)));
        setCommand.then(builder);
    }

    private static int setBooleanConfigValue(final GitConfigKey key, final String keyDisplay, final boolean newValue, final CommandContext<ServerCommandSource> cc) {
        try (UserLogger ulog = ulog(cc)) {
            final Path worldSaveDir = mod().getWorldDirectory();
            final RepoFactory rf = rf();
            if (rf.isGitRepo(worldSaveDir)) {
                try (Repo repo = rf.load(worldSaveDir)) {
                    repo.getConfig().updater().set(key, newValue).save();
                    ulog.message(raw(keyDisplay + " = " + newValue));
                } catch (Exception e) {
                    ulog.internalError(e);
                    return FAILURE;
                }
            }
        }
        return SUCCESS;
    }

    // ======================================================================
    // Integer config values

    private static void registerIntegerConfigValue(final GitConfigKey key, final String keyDisplayOrNull, final String argName, final LiteralArgumentBuilder<ServerCommandSource> setCommand) {
        final String keyDisplay = keyDisplayOrNull != null ? keyDisplayOrNull : key.getSettingName();
        final LiteralArgumentBuilder<ServerCommandSource> builder = literal(keyDisplay);
        builder.then(argument(argName, IntegerArgumentType.integer()).
                executes(cc -> setIntegerConfigValue(key, keyDisplay, argName, cc)));
        setCommand.then(builder);
    }

    private static int setIntegerConfigValue(final GitConfigKey key, final String keyDisplay, final String argName, final CommandContext<ServerCommandSource> cc) {
        try (UserLogger ulog = ulog(cc)) {
            final Path worldSaveDir = mod().getWorldDirectory();
            final RepoFactory rf = rf();
            if (rf.isGitRepo(worldSaveDir)) {
                try (Repo repo = rf.load(worldSaveDir)) {
                    final Integer newValue = cc.getArgument(argName, Integer.class);
                    repo.getConfig().updater().set(key, newValue).save();
                    ulog.message(raw(keyDisplay + " = " + newValue));
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

    private static void registerStringConfigValue(final GitConfigKey key, final String keyDisplayOrNull, final String argName, final LiteralArgumentBuilder<ServerCommandSource> setCommand) {
        final String keyDisplay = keyDisplayOrNull != null ? keyDisplayOrNull : key.getSettingName();
        final LiteralArgumentBuilder<ServerCommandSource> builder = literal(keyDisplay);
        builder.then(argument(argName, StringArgumentType.greedyString()).
                executes(cc -> setStringConfigValue(key, keyDisplay, argName, cc)));
        setCommand.then(builder);
    }

    private static int setStringConfigValue(final GitConfigKey key, final String keyDisplay, final String argName, final CommandContext<ServerCommandSource> cc) {
        try (UserLogger ulog = ulog(cc)) {
            final Path worldSaveDir = mod().getWorldDirectory();
            final RepoFactory rf = rf();
            if (rf.isGitRepo(worldSaveDir)) {
                try (Repo repo = rf.load(worldSaveDir)) {
                    final String newValue = cc.getArgument(argName, String.class);
                    repo.getConfig().updater().set(key, newValue).save();
                    ulog.message(raw(keyDisplay + " = " + newValue));
                } catch (Exception e) {
                    ulog.internalError(e);
                    return FAILURE;
                }
            }
        }
        return SUCCESS;
    }

    // ======================================================================
    // Selection config values

    private static void registerSelectConfigValue(GitConfigKey key, String keyDisplayOrNull, List<String> selections, final LiteralArgumentBuilder<ServerCommandSource> setCommand) {
        final String keyDisplay = keyDisplayOrNull != null ? keyDisplayOrNull : key.getSettingName();
        final LiteralArgumentBuilder<ServerCommandSource> builder = literal(keyDisplay);
        for(final String selection : selections) {
            builder.then(literal(selection).executes(cc -> setSelectionConfigValue(key, keyDisplay, selection, cc)));
        }
        setCommand.then(builder);
    }

    private static int setSelectionConfigValue(final GitConfigKey key, final String keyDisplay, final String newValue, final CommandContext<ServerCommandSource> cc) {
        try (UserLogger ulog = ulog(cc)) {
            final Path worldSaveDir = mod().getWorldDirectory();
            if (rf().isGitRepo(worldSaveDir)) {
                try (final Repo repo = rf().load(worldSaveDir)) {
                    repo.getConfig().updater().set(key, newValue).save();
                    ulog.message(raw(keyDisplay + " = " + newValue));
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
