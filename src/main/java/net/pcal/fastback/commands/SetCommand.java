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
import net.pcal.fastback.config.FastbackConfigKey;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.config.GitConfigKey;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.mod.Mod;
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.repo.RepoFactory;
import net.pcal.fastback.retention.RetentionPolicy;
import net.pcal.fastback.retention.RetentionPolicyCodec;
import net.pcal.fastback.retention.RetentionPolicyType;
import net.pcal.fastback.utils.EnvironmentUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.getArgumentNicely;
import static net.pcal.fastback.commands.Commands.missingArgument;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.config.FastbackConfigKey.*;
import static net.pcal.fastback.config.OtherConfigKey.REMOTE_PUSH_URL;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserLogger.ulog;
import static net.pcal.fastback.logging.UserMessage.*;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
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
        registerBooleanConfigValue(IS_MODS_BACKUP_ENABLED, null, sc);
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

        registerSetRetentionCommand(LOCAL_RETENTION_POLICY, null, sc);
        registerSetRetentionCommand(REMOTE_RETENTION_POLICY, null, sc);

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
                    final GitConfig conf = repo.getConfig();
                    boolean current = conf.getBoolean(key);
                    if (current == newValue) {
                        ulog.message(raw("No change.")); // FIXME i18n
                    } else {
                        if (key == IS_NATIVE_GIT_ENABLED) {
                            if (!validateNativeGitChange(newValue, repo, ulog)) return FAILURE;
                        }
                        repo.getConfig().updater().set(key, newValue).save();
                        ulog.message(raw(keyDisplay + " = " + newValue));
                    }
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
        for (final String selection : selections) {
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


    // ======================================================================
    // Retention policy commands

    /**
     * Register a 'set retention' command that tab completes with all the policies and the policy arguments.
     * Broken out as a helper methods so this logic can be shared by set-retention and set-remote-retention.
     * <p>
     * FIXME? The command parsing here could be more user-friendly.  Not really clear how to implement
     * argument defaults.  Also a lot of noise from bugs like this: https://bugs.mojang.com/browse/MC-165562
     * Just generally not sure how to beat brigadier into submission here.
     */
    private static void registerSetRetentionCommand(final FastbackConfigKey key,
                                                    final BiFunction<CommandContext<ServerCommandSource>, RetentionPolicyType, Integer> setPolicyFn,
                                                    final LiteralArgumentBuilder<ServerCommandSource> argb) {
        final LiteralArgumentBuilder<ServerCommandSource> retainCommand = literal(key.getSettingName());
        for (final RetentionPolicyType rpt : RetentionPolicyType.getAvailable()) {
            final LiteralArgumentBuilder<ServerCommandSource> policyCommand = literal(rpt.getCommandName());
            policyCommand.executes(cc -> setRetentionPolicy(cc, rpt, key));
            if (rpt.getParameters() != null) {
                for (RetentionPolicyType.Parameter<?> param : rpt.getParameters()) {
                    policyCommand.then(argument(param.name(), param.type()).
                            executes(cc -> setRetentionPolicy(cc, rpt, key)));
                }
            }
            retainCommand.then(policyCommand);
        }
        argb.then(retainCommand);
    }


    /**
     * Does the work to encode a policy configuration and set it in git configuration.
     * Broken out as a helper methods so this logic can be shared by set-retention and set-remote-retention.
     * <p>
     * TODO this should probably move to Repo.
     */
    public static int setRetentionPolicy(final CommandContext<ServerCommandSource> cc,
                                         final RetentionPolicyType rpt,
                                         final FastbackConfigKey confKey) {
        final UserLogger ulog = ulog(cc);
        final Path worldSaveDir = mod().getWorldDirectory();
        try (final Repo repo = rf().load(worldSaveDir)) {
            final Map<String, String> config = new HashMap<>();
            for (final RetentionPolicyType.Parameter<?> p : rpt.getParameters()) {
                final Object val = getArgumentNicely(p.name(), p.clazz(), cc, ulog);
                if (val == null) return FAILURE;
                config.put(p.name(), String.valueOf(val));
            }
            final String encodedPolicy = RetentionPolicyCodec.INSTANCE.encodePolicy(rpt, config);
            final RetentionPolicy rp =
                    RetentionPolicyCodec.INSTANCE.decodePolicy(RetentionPolicyType.getAvailable(), encodedPolicy);
            if (rp == null) {
                syslog().error("Failed to decode policy " + encodedPolicy, new Exception());
                return FAILURE;
            }
            final GitConfig conf = repo.getConfig();
            conf.updater().set(confKey, encodedPolicy).save();
            ulog.message(localized("fastback.chat.retention-policy-set"));
            ulog.message(rp.getDescription());
            return SUCCESS;
        } catch (Exception e) {
            syslog().error("Failed to set retention policy", e);
            return FAILURE;
        }
    }

    // ======================================================================
    // Special validations

    /**
     * FIXME i18n
     */
    private static boolean validateNativeGitChange(final boolean newValue, final Repo repo, final UserLogger user) throws IOException {
        if (newValue) {
            if (!EnvironmentUtils.isNativeGitInstalled()) {
                user.message(styledRaw("Native git is not installed on your machine.  Please install it and try again.", ERROR)); //FIXME i18n
                return false;
            }
        }
        if (!repo.getLocalSnapshots().isEmpty()) {
            user.message(styledRaw("You can't change " +IS_NATIVE_GIT_ENABLED.getSettingName()+" once you've " +
                    "made a backup.  If you want to delete your current backups and start over, delete the .git directory in your world folder. ", ERROR));
            return false;
        }
        return true;
    }



}
