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
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.CommandSourceLogger;
import net.pcal.fastback.logging.CompositeLogger;
import net.pcal.fastback.logging.Logger;
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
        EnableCommand.register(argb, ctx);
        DisableCommand.register(argb, ctx);
        LocalCommand.register(argb, ctx);
        FullCommand.register(argb, ctx);
        InfoCommand.register(argb, ctx);

        RestoreCommand.register(argb, ctx);
        CreateFileRemoteCommand.register(argb, ctx);
        SetRemoteCommand.register(argb, ctx);
        SetAutosaveActionCommand.register(argb, ctx);
        SetShutdownActionCommand.register(argb, ctx);

        SetRetentionCommand.register(argb, ctx);
        PruneCommand.register(argb, ctx);
        PurgeCommand.register(argb, ctx);
        GcCommand.register(argb, ctx);
        ListCommand.register(argb, ctx);

        HelpCommand.register(argb, ctx);
        if (ctx.isExperimentalCommandsEnabled()) {
            SaveCommand.register(argb, ctx);
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, regAccess, env) -> dispatcher.register(argb));
    }

    public static Logger commandLogger(final ModContext ctx, final ServerCommandSource scs) {
        return CompositeLogger.of(ctx.getLogger(), new CommandSourceLogger(ctx, scs));
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
                log.notifyError(localized("fastback.notify.not-enabled"));
                return;
            }
            try (final Git git = Git.open(worldSaveDir.toFile())) {
                final WorldConfig worldConfig = WorldConfig.load(git);
                if (!worldConfig.isBackupEnabled()) {
                    log.notifyError(localized("fastback.notify.not-enabled"));
                } else {
                    op.execute(git);
                }
            } catch (Exception e) {
                log.internalError("Command execution failed.", e);
            }
        });
    }
}


