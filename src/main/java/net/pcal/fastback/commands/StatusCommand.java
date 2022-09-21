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

import java.io.File;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.text.Text.translatable;
import static net.pcal.fastback.commands.Commands.*;
import static net.pcal.fastback.utils.FileUtils.getDirDisplaySize;

public class StatusCommand {

    private static final String COMMAND_NAME = "status";

    public static void register(LiteralArgumentBuilder<ServerCommandSource> argb, ModContext ctx) {
        final StatusCommand rc = new StatusCommand(ctx);
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(rc::execute));
    }

    private final ModContext ctx;

    private StatusCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        return executeStandardNew(this.ctx, cc, (git, wc, log) -> {
            this.ctx.getExecutorService().execute(() -> {
                if (wc.isBackupEnabled()) {
                    log.notify(translatable("fastback.notify.status-local-enabled"));
                } else {
                    log.notify(translatable("fastback.notify.status-local-disabled"));
                }
                if (wc.isRemoteBackupEnabled()) {
                    log.notify(translatable("fastback.notify.status-remote-enabled"));
                } else {
                    log.notify(translatable("fastback.notify.status-remote-disabled"));
                }
                if (wc.isRemoteBackupEnabled()) {
                    String url = wc.getRemotePushUrl();
                    if (url == null) {
                        log.notifyError(translatable("fastback.notify.status-remote-url-missing"));
                    } else {
                        log.notify(translatable("fastback.notify.status-remote-url", url));
                    }
                }
                if (wc.isShutdownBackupEnabled()) {
                    log.notify(translatable("fastback.notify.status-shutdown-enabled"));
                } else {
                    log.notify(translatable("fastback.notify.status-shutdown-disabled"));
                }
                final File gitDir = git.getRepository().getDirectory();
                log.notify(translatable("fastback.notify.status-backup-size", getDirDisplaySize(gitDir)));
            });
            return SUCCESS;
        });
    }
}
