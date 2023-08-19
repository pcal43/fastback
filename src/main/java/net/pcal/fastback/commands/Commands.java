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
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.ModContext.ExecutionLock;
import net.pcal.fastback.repo.RepoConfig;
import net.pcal.fastback.logging.CommandSourceLogger;
import net.pcal.fastback.logging.CompositeLogger;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.SaveScreenLogger;
import org.eclipse.jgit.api.Git;

import java.nio.file.Path;
import java.util.function.Predicate;

import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.utils.GitUtils.isGitRepo;

public class Commands {

    static String BACKUP_COMMAND_PERM = "fastback.command";

    static int FAILURE = 0;
    static int SUCCESS = 1;

    public static void registerCommands(final ModContext ctx, final String cmd) {
        final LiteralArgumentBuilder<ServerCommandSource> argb = LiteralArgumentBuilder.<ServerCommandSource>literal(cmd).
                requires(Permissions.require(BACKUP_COMMAND_PERM, ctx.getDefaultPermLevel()));
        EnableCommand.INSTANCE.register(argb, ctx);
        DisableCommand.INSTANCE.register(argb, ctx);
        LocalCommand.INSTANCE.register(argb, ctx);
        FullCommand.INSTANCE.register(argb, ctx);
        InfoCommand.INSTANCE.register(argb, ctx);

        RestoreCommand.INSTANCE.register(argb, ctx);
        CreateFileRemoteCommand.INSTANCE.register(argb, ctx);
        SetRemoteCommand.INSTANCE.register(argb, ctx);
        SetAutobackActionCommand.INSTANCE.register(argb, ctx);
        SetAutobackWaitCommand.INSTANCE.register(argb, ctx);
        SetShutdownActionCommand.INSTANCE.register(argb, ctx);

        SetRetentionCommand.INSTANCE.register(argb, ctx);
        SetRemoteRetentionCommand.INSTANCE.register(argb, ctx);

        PruneCommand.INSTANCE.register(argb, ctx);
        DeleteCommand.INSTANCE.register(argb, ctx);
        GcCommand.INSTANCE.register(argb, ctx);
        ListCommand.INSTANCE.register(argb, ctx);

        RemoteListCommand.INSTANCE.register(argb, ctx);
        RemoteDeleteCommand.INSTANCE.register(argb, ctx);
        RemotePruneCommand.INSTANCE.register(argb, ctx);
        RemoteRestoreCommand.INSTANCE.register(argb, ctx);

        HelpCommand.INSTANCE.register(argb, ctx);
        if (ctx.isExperimentalCommandsEnabled()) {
            SaveCommand.INSTANCE.register(argb, ctx);
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, regAccess, env) -> dispatcher.register(argb));
    }

    public static Logger commandLogger(final ModContext ctx, final ServerCommandSource scs) {
        return CompositeLogger.of(ctx.getLogger(), new CommandSourceLogger(ctx, scs), new SaveScreenLogger(ctx));
    }

    public static String subcommandPermName(String subcommandName) {
        return "fastback.command." + subcommandName;
    }

    public static Predicate<ServerCommandSource> subcommandPermission(ModContext ctx, String subcommandName) {
        return Permissions.require(subcommandPermName(subcommandName), ctx.getDefaultPermLevel());
    }

    interface GitOp {
        void execute(Git git) throws Exception;
    }

    static void gitOp(final ModContext ctx, final ExecutionLock lock, final Logger log, final GitOp op) {
        ctx.execute(lock, log, () -> {
            final Path worldSaveDir = ctx.getWorldDirectory();
            if (!isGitRepo(worldSaveDir)) {
                log.chatError(localized("fastback.chat.not-enabled"));
                return;
            }
            try (final Git git = Git.open(worldSaveDir.toFile())) {
                final RepoConfig repoConfig = RepoConfig.load(git);
                if (!repoConfig.isBackupEnabled()) {
                    log.chatError(localized("fastback.chat.not-enabled"));
                } else {
                    op.execute(git);
                }
            } catch (Exception e) {
                log.internalError("Command execution failed.", e);
            } finally {
                log.hud(null); // ensure we always clear the hud text
            }
        });
    }
}


