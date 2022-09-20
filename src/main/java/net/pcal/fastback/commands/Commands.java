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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.CommandSourceLogger;
import net.pcal.fastback.logging.CompositeLogger;
import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.function.Predicate;

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
        StatusCommand.register(argb, ctx);
        RestoreCommand.register(argb, ctx);
        PurgeCommand.register(argb, ctx);
        NowCommand.register(argb, ctx);
        ListCommand.register(argb, ctx);
        RemoteCommand.register(argb, ctx);
        FileRemoteCommand.register(argb, ctx);
        ShutdownCommand.register(argb, ctx);
        UuidCommand.register(argb, ctx);
        VersionCommand.register(argb, ctx);
        HelpCommand.register(argb, ctx);
        if (ctx.isExperimentalCommandsEnabled()) {
            SaveCommand.register(argb, ctx);
            GcCommand.register(argb, ctx);
            GcInfoCommand.register(argb, ctx);
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, regAccess, env) -> dispatcher.register(argb));
    }

    public static Logger commandLogger(final ModContext ctx, final CommandContext<ServerCommandSource> cc) {
        return CompositeLogger.of(
                ctx.getLogger(),
                new CommandSourceLogger(cc.getSource())
        );
    }

    public static String subcommandPermName(String subcommandName) {
        return "fastback.command." + subcommandName;
    }

    public static @NotNull Predicate<ServerCommandSource> subcommandPermission(ModContext ctx, String subcommandName) {
        return Permissions.require(subcommandPermName(subcommandName), ctx.getDefaultPermLevel());
    }

    interface CommandLogic {
        int execute(StoredConfig gitConfig, WorldConfig worldConfig, Logger logger)
                throws IOException, GitAPIException, ParseException;
    }

    static int executeStandard(final ModContext ctx, final CommandContext<ServerCommandSource> cc, CommandLogic sub) {
        final MinecraftServer server = cc.getSource().getServer();
        final Logger logger = commandLogger(ctx, cc);
        final Path worldSaveDir = ctx.getWorldSaveDirectory(server);
        if (!isGitRepo(worldSaveDir)) {
            logger.notifyError(Text.translatable("fastback.notify.not-enabled"));
            return FAILURE;
        }
        try (final Git git = Git.open(worldSaveDir.toFile())) {
            final StoredConfig gitConfig = git.getRepository().getConfig();
            final WorldConfig worldConfig = WorldConfig.load(worldSaveDir, gitConfig);
            if (!worldConfig.isBackupEnabled()) {
                logger.notifyError(Text.translatable("fastback.notify.not-enabled"));
                return FAILURE;
            }
            return sub.execute(gitConfig, worldConfig, logger);
        } catch (Exception e) {
            logger.internalError("Command execution failed.", e);
            return FAILURE;
        }
    }
}
