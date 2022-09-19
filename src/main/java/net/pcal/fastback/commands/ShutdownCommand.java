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
import net.minecraft.text.Text;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.text.Text.translatable;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.executeStandard;
import static net.pcal.fastback.commands.Commands.subcommandPermission;

public class ShutdownCommand {

    private static final String COMMAND_NAME = "shutdown";
    private static final String ENABLE_ARGUMENT = "enable";
    private static final String DISABLE_ARGUMENT = "disable";

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final ShutdownCommand c = new ShutdownCommand(ctx);
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(c::show).then(
                                literal(ENABLE_ARGUMENT).executes(c::enable)).then(
                                literal(DISABLE_ARGUMENT).executes(c::disable))
        );
    }

    private final ModContext ctx;

    private ShutdownCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int show(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final boolean enabled = wc.isShutdownBackupEnabled();
            if (enabled) {
                log.notify(translatable("fastback.notify.shutdown-currently-enabled"));
            } else {
                log.notify(translatable("fastback.notify.shutdown-currently-disabled"));
            }
            return SUCCESS;
        });
    }

    private int enable(final CommandContext<ServerCommandSource> cc) {
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

    private int disable(final CommandContext<ServerCommandSource> cc) {
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
}
