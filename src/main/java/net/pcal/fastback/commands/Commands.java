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
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.mod.Mod;
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.repo.RepoFactory;
import net.pcal.fastback.utils.Executor.ExecutionLock;

import java.nio.file.Path;
import java.util.function.Predicate;

import static net.pcal.fastback.config.FastbackConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.logging.UserMessage.styledLocalized;
import static net.pcal.fastback.mod.Mod.mod;
import static net.pcal.fastback.utils.Executor.executor;

public class Commands {

    static String BACKUP_COMMAND_PERM = "fastback.command";

    static int FAILURE = 0;
    static int SUCCESS = 1;

    public static void registerCommands(final Mod mod) {
        final LiteralArgumentBuilder<ServerCommandSource> root = LiteralArgumentBuilder.<ServerCommandSource>literal("backup").
                requires(Permissions.require(BACKUP_COMMAND_PERM, mod.getDefaultPermLevel())).
                executes(HelpCommand::generalHelp);
        InitCommand.INSTANCE.register(root,mod());
        LocalCommand.INSTANCE.register(root,mod());
        FullCommand.INSTANCE.register(root,mod());
        InfoCommand.INSTANCE.register(root,mod());

        RestoreCommand.INSTANCE.register(root,mod());
        CreateFileRemoteCommand.INSTANCE.register(root,mod());

        PruneCommand.INSTANCE.register(root,mod());
        DeleteCommand.INSTANCE.register(root,mod());
        GcCommand.INSTANCE.register(root,mod());
        ListCommand.INSTANCE.register(root,mod());
        PushCommand.INSTANCE.register(root,mod());

        RemoteListCommand.INSTANCE.register(root,mod());
        RemoteDeleteCommand.INSTANCE.register(root,mod());
        RemotePruneCommand.INSTANCE.register(root,mod());
        RemoteRestoreCommand.INSTANCE.register(root,mod());

        SetCommand.INSTANCE.register(root,mod());

        HelpCommand.INSTANCE.register(root,mod());

        CommandRegistrationCallback.EVENT.register((dispatcher, regAccess, env) -> dispatcher.register(root));
    }

    @Deprecated
    public static UserLogger commandLogger(final Mod mod, final ServerCommandSource scs) {
        return UserLogger.ulog(scs);
    }

    public static String subcommandPermName(String subcommandName) {
        return "fastback.command." + subcommandName;
    }

    @Deprecated
    public static Predicate<ServerCommandSource> subcommandPermission(Mod mod, String subcommandName) {
        return subcommandPermission(subcommandName);
    }

    public static Predicate<ServerCommandSource> subcommandPermission(String subcommandName) {
        return Permissions.require(subcommandPermName(subcommandName), mod().getDefaultPermLevel());
    }

    /**
     * Retrieve a command argument. If they forgot to provide it, return null
     * and log a helpful message rather than blowing up the world.  This is needed in the
     * cases where the list of arguments is dynamic (e.g., retention policies) and we can't
     * rely on brigadier's static parse trees.
     */
    public static <V> V getArgumentNicely(final String argName, final Class<V> clazz, final CommandContext<?> cc, UserLogger log) {
        try {
            return cc.getArgument(argName, clazz);
        } catch (IllegalArgumentException iae) {
            missingArgument(argName, log);
            return null;
        }
    }

    @Deprecated
    public static int missingArgument(final String argName, final Mod mod, final CommandContext<ServerCommandSource> cc) {
        return missingArgument(argName, commandLogger(mod, cc.getSource()));
    }

    public static int missingArgument(final String argName, final CommandContext<ServerCommandSource> cc) {
        return missingArgument(argName, UserLogger.ulog(cc));
    }

    public static int missingArgument(final String argName, final UserLogger log) {
        log.message(styledLocalized("fastback.chat.missing-argument", ERROR, argName));
        return FAILURE;
    }

    interface GitOp {
        void execute(Repo repo) throws Exception;
    }

    static void gitOp(final ExecutionLock lock, final UserLogger ulog, final GitOp op) {
        try {
            executor().execute(lock, ulog, () -> {
                final Path worldSaveDir = mod().getWorldDirectory();
                final RepoFactory rf = RepoFactory.rf();
                if (!rf.isGitRepo(worldSaveDir)) { // FIXME this is not the right place for these checks
                    ulog.message(styledLocalized("fastback.chat.not-enabled", ERROR));
                    return;
                }
                try (final Repo repo = rf.load(worldSaveDir)) {
                    final GitConfig repoConfig = repo.getConfig();
                    if (!repoConfig.getBoolean(IS_BACKUP_ENABLED)) {
                        ulog.message(styledLocalized("fastback.chat.not-enabled", ERROR));
                    } else {
                        op.execute(repo);
                    }
                } catch (Exception e) {
                    ulog.message(styledLocalized("fastback.chat.internal-error", ERROR));
                    syslog().error(e);
                } finally {
                    mod().clearHudText();
                }
            });
        } catch(Exception e) {
            ulog.internalError();
            syslog().error(e);
        }
    }

    @Deprecated
    static void gitOp(final Mod mod, final ExecutionLock lock, final UserLogger ulog, final GitOp op) {
        try {
            executor().execute(lock, ulog, () -> {
                final Path worldSaveDir = mod.getWorldDirectory();
                final RepoFactory rf = RepoFactory.rf();
                if (!rf.isGitRepo(worldSaveDir)) { // FIXME this is not the right place for these checks
                    ulog.message(styledLocalized("fastback.chat.not-enabled", ERROR));
                    return;
                }
                try (final Repo repo = rf.load(worldSaveDir)) {
                    final GitConfig repoConfig = repo.getConfig();
                    if (!repoConfig.getBoolean(IS_BACKUP_ENABLED)) {
                        ulog.message(styledLocalized("fastback.chat.not-enabled", ERROR));
                    } else {
                        op.execute(repo);
                    }
                } catch (Exception e) {
                    ulog.message(styledLocalized("fastback.chat.internal-error", ERROR));
                    syslog().error(e);
                } finally {
                    mod.clearHudText();
                }
            });
        } catch(Exception e) {
            ulog.internalError();
            syslog().error(e);
        }
    }
}


