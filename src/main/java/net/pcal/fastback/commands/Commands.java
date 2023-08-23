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

import static net.pcal.fastback.commands.HelpCommand.help;
import static net.pcal.fastback.config.GitConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.logging.UserMessage.styledLocalized;

public class Commands {

    static String BACKUP_COMMAND_PERM = "fastback.command";

    static int FAILURE = 0;
    static int SUCCESS = 1;

    public static void registerCommands(final Mod ctx) {
        final LiteralArgumentBuilder<ServerCommandSource> root = LiteralArgumentBuilder.<ServerCommandSource>literal("backup").
                requires(Permissions.require(BACKUP_COMMAND_PERM, ctx.getDefaultPermLevel())).
                executes(cc->help(ctx, cc));
        EnableCommand.INSTANCE.register(root, ctx);
        DisableCommand.INSTANCE.register(root, ctx);
        LocalCommand.INSTANCE.register(root, ctx);
        FullCommand.INSTANCE.register(root, ctx);
        InfoCommand.INSTANCE.register(root, ctx);

        RestoreCommand.INSTANCE.register(root, ctx);
        CreateFileRemoteCommand.INSTANCE.register(root, ctx);
        SetRemoteCommand.INSTANCE.register(root, ctx);
        SetAutobackActionCommand.INSTANCE.register(root, ctx);
        SetAutobackWaitCommand.INSTANCE.register(root, ctx);
        SetShutdownActionCommand.INSTANCE.register(root, ctx);

        SetRetentionCommand.INSTANCE.register(root, ctx);
        SetRemoteRetentionCommand.INSTANCE.register(root, ctx);

        PruneCommand.INSTANCE.register(root, ctx);
        DeleteCommand.INSTANCE.register(root, ctx);
        GcCommand.INSTANCE.register(root, ctx);
        ListCommand.INSTANCE.register(root, ctx);

        RemoteListCommand.INSTANCE.register(root, ctx);
        RemoteDeleteCommand.INSTANCE.register(root, ctx);
        RemotePruneCommand.INSTANCE.register(root, ctx);
        RemoteRestoreCommand.INSTANCE.register(root, ctx);

        SetCommand.INSTANCE.register(root, ctx);

        HelpCommand.INSTANCE.register(root, ctx);

        CommandRegistrationCallback.EVENT.register((dispatcher, regAccess, env) -> dispatcher.register(root));
    }

    public static UserLogger commandLogger(final Mod ctx, final ServerCommandSource scs) {
        return new CommandSourceLogger(ctx, scs);
    }

    public static String subcommandPermName(String subcommandName) {
        return "fastback.command." + subcommandName;
    }

    public static Predicate<ServerCommandSource> subcommandPermission(Mod ctx, String subcommandName) {
        return Permissions.require(subcommandPermName(subcommandName), ctx.getDefaultPermLevel());
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
        } catch(IllegalArgumentException iae) {
            missingArgument(argName, log);
            return null;
        }
    }

    public static int missingArgument(final String argName, final Mod ctx, final CommandContext<ServerCommandSource> cc) {
        return missingArgument(argName, commandLogger(ctx, cc.getSource()));
    }

    public static int missingArgument(final String argName, final UserLogger log) {
        log.chat(styledLocalized("fastback.chat.missing-argument", ERROR, argName));
        return FAILURE;
    }

    interface GitOp {
        void execute(Repo repo) throws Exception;
    }

    static void gitOp(final Mod mod, final ExecutionLock lock, final UserLogger ulog, final GitOp op) {
        mod.getExecutor().execute(lock, ulog, () -> {
            final Path worldSaveDir = mod.getWorldDirectory();
            final RepoFactory rf = RepoFactory.get();
            if (!rf.isGitRepo(worldSaveDir)) {
                ulog.chat(styledLocalized("fastback.chat.not-enabled", ERROR));
                return;
            }
            try (final Repo repo = rf.load(worldSaveDir, mod)) {
                final GitConfig repoConfig = repo.getConfig();
                if (!repoConfig.getBoolean(IS_BACKUP_ENABLED)) {
                    ulog.chat(styledLocalized("fastback.chat.not-enabled", ERROR));
                } else {
                    op.execute(repo);
                }
            } catch (Exception e) {
                syslog().error("Command execution failed.", e);
                ulog.chat(styledLocalized("fastback.chat.internal-error", ERROR));
            } finally {
                mod.clearHudText();
            }
        });
    }
}


