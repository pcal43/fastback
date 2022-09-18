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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.text.Text.translatable;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.executeStandard;
import static net.pcal.fastback.commands.Commands.subcommandPermission;

public class RemoteCommand {

    private static final String COMMAND_NAME = "remote";
    private static final String ENABLE_ARGUMENT = "enable";
    private static final String DISABLE_ARGUMENT = "disable";
    private static final String URL_ARGUMENT = "remote-url";

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final RemoteCommand c = new RemoteCommand(ctx);
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(c::showRemote).then(
                                literal(ENABLE_ARGUMENT).executes(c::enable)).then(
                                literal(DISABLE_ARGUMENT).executes(c::disable)).then(
                                argument(URL_ARGUMENT, StringArgumentType.greedyString()).
                                        executes(c::setRemoteUrl))
        );
    }

    private final ModContext ctx;

    private RemoteCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int showRemote(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final String remoteUrl = wc.getRemotePushUrl();
            final boolean enabled = wc.isRemoteBackupEnabled();
            if (enabled && remoteUrl != null) {
                log.notify(translatable("fastback.notify.remote-enabled", remoteUrl));
                return SUCCESS;
            } else {
                log.notifyError(translatable("fastback.notify.remote-disabled"));
                if (remoteUrl != null) {
                    log.notifyError(translatable("fastback.notify.remote-how-to-enable", remoteUrl));
                } else {
                    log.notify(translatable("fastback.notify.remote-how-to-enable-no-url"));
                }
                return FAILURE;
            }
        });
    }

    private int enable(final CommandContext<ServerCommandSource> cc) {
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

    private int disable(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final boolean currentEnabled = wc.isRemoteBackupEnabled();
            if (!currentEnabled) {
                log.notifyError("Remote backups are already disabled.");
                return FAILURE;
            } else {
                WorldConfig.setRemoteBackupEnabled(gitc, false);
                gitc.save();
                log.notifyError("Remote backups disabled.");
                return SUCCESS;
            }
        });
    }

    private int setRemoteUrl(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final String newUrl = cc.getArgument(URL_ARGUMENT, String.class);
            final String currentUrl = wc.getRemotePushUrl();
            final boolean currentEnable = wc.isRemoteBackupEnabled();
            if (currentUrl != null && currentUrl.equals(newUrl)) {
                if (currentEnable) {
                    log.notify(translatable("fastback.notify.remote-already-enabled", newUrl));
                    return SUCCESS;
                } else {
                    WorldConfig.setRemoteBackupEnabled(gitc, true);
                    gitc.save();
                    log.notify(translatable("fastback.notify.remote-enabled", newUrl));
                }
            } else {
                WorldConfig.setRemoteUrl(gitc, newUrl);
                if (currentEnable) {
                    log.notify(translatable("fastback.notify.remote-changed", newUrl));
                } else {
                    WorldConfig.setRemoteBackupEnabled(gitc, true);
                    log.notify(translatable("fastback.notify.remote-enabled", newUrl));
                }
                gitc.save();
            }
            return SUCCESS;
        });
    }

}
