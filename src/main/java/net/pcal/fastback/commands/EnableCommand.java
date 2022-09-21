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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.text.Text.translatable;
import static net.pcal.fastback.WorldConfig.doWorldMaintenance;
import static net.pcal.fastback.commands.Commands.*;

public class EnableCommand {

    private static final String COMMAND_NAME = "enable";
    private static final String SHUTDOWN = "shutdown";
    private static final String REMOTE = "remote";

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final EnableCommand c = new EnableCommand(ctx);
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(c::enable).then(
                                literal(SHUTDOWN).executes(c::enableShutdown)).then(
                                literal(REMOTE).executes(c::enableRemote))
        );
    }

    private final ModContext ctx;

    private EnableCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int enable(final CommandContext<ServerCommandSource> cc) {
        final MinecraftServer server = cc.getSource().getServer();
        final Logger logger = commandLogger(ctx, cc);
        final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
        try (final Git git = Git.init().setDirectory(worldSaveDir.toFile()).call()) {
            doWorldMaintenance(git, logger);
            final StoredConfig config = git.getRepository().getConfig();
            final WorldConfig worldConfig = WorldConfig.load(worldSaveDir, config);
            if (worldConfig.isBackupEnabled() && worldConfig.isShutdownBackupEnabled()) {
                logger.notifyError(translatable("fastback.notify.enable-already-enabled"));
                return FAILURE;
            } else {
                WorldConfig.setBackupEnabled(config, true);
                WorldConfig.setShutdownBackupEnabled(config, true);
                config.save();
                logger.notify(translatable("fastback.notify.enable-done"));
                return SUCCESS;
            }
        } catch (GitAPIException | IOException e) {
            logger.internalError("Error enabling backups", e);
            return FAILURE;
        }
    }

    private int enableShutdown(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final boolean enabled = wc.isShutdownBackupEnabled();
            if (enabled) {
                log.notifyError(translatable("fastback.notify.shutdown-currently-enabled"));
                return FAILURE;
            } else {
                WorldConfig.setShutdownBackupEnabled(gitc, true);
                gitc.save();
                log.notifyError(translatable("fastback.notify.shutdown-enabled"));
                return SUCCESS;
            }
        });
    }

    private int enableRemote(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final String currentUrl = wc.getRemotePushUrl();
            final boolean currentEnabled = wc.isRemoteBackupEnabled();
            if (currentUrl == null) {
                log.notifyError(translatable("fastback.notify.remote-no-url"));
                return FAILURE;
            } else if (currentEnabled) {
                log.notifyError(translatable("fastback.notify.remote-already-enabled", currentUrl));
                return FAILURE;
            } else {
                log.notify(translatable("fastback.notify.remote-enabled", currentUrl));
                WorldConfig.setRemoteBackupEnabled(gitc, true);
                gitc.save();
                return SUCCESS;
            }
        });
    }
}
