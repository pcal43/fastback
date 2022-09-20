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
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.text.Text.translatable;
import static net.pcal.fastback.commands.Commands.*;

public class DisableCommand {

    private static final String COMMAND_NAME = "disable";
    private static final String SHUTDOWN = "shutdown";
    private static final String REMOTE = "remote";

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final DisableCommand c = new DisableCommand(ctx);
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(c::disable).then(
                                literal(SHUTDOWN).executes(c::disableShutdown)).then(
                                literal(REMOTE).executes(c::disableRemote))
        );
    }

    private final ModContext ctx;

    private DisableCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int disable(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            WorldConfig.setBackupEnabled(gitc, false);
            WorldConfig.setShutdownBackupEnabled(gitc, false);
            gitc.save();
            log.notify(translatable("fastback.notify.disable-done"));
            return SUCCESS;
        });
    }

    private int disableShutdown(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final boolean enabled = wc.isShutdownBackupEnabled();
            if (!enabled) {
                log.notifyError(translatable("fastback.notify.shutdown-currently-disabled"));
                return FAILURE;
            } else {
                WorldConfig.setShutdownBackupEnabled(gitc, false);
                gitc.save();
                log.notifyError(translatable("fastback.notify.shutdown-disabled"));
                return SUCCESS;
            }
        });
    }

    private int disableRemote(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final boolean currentEnabled = wc.isRemoteBackupEnabled();
            if (!currentEnabled) {
                log.notifyError(translatable("fastback.notify.remote-already-disabled"));
                return FAILURE;
            } else {
                WorldConfig.setRemoteBackupEnabled(gitc, false);
                gitc.save();
                log.notifyError(translatable("fastback.notify.remote-disabled"));
                return SUCCESS;
            }
        });
    }
}

