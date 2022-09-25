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
import net.pcal.fastback.retention.RetentionPolicy;
import net.pcal.fastback.retention.RetentionPolicyCodec;
import net.pcal.fastback.retention.RetentionPolicyType;

import java.io.File;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.executeStandardNew;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.utils.FileUtils.getDirDisplaySize;

public class InfoCommand {

    private static final String COMMAND_NAME = "info";

    public static void register(LiteralArgumentBuilder<ServerCommandSource> argb, ModContext ctx) {
        final InfoCommand rc = new InfoCommand(ctx);
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(rc::execute));
    }

    private final ModContext ctx;

    private InfoCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        return executeStandardNew(this.ctx, cc, (git, wc, log) -> {
            this.ctx.getExecutorService().execute(() -> {
                log.notify(localized("fastback.notify.info-fastback-version", this.ctx.getModVersion()));
                log.notify(localized("fastback.notify.info-uuid", wc.worldUuid()));
                if (wc.isBackupEnabled()) {
                    log.notify(localized("fastback.notify.info-local-enabled"));
                } else {
                    log.notify(localized("fastback.notify.info-local-disabled"));
                }
                if (wc.isRemoteBackupEnabled()) {
                    log.notify(localized("fastback.notify.info-remote-enabled"));
                } else {
                    log.notify(localized("fastback.notify.info-remote-disabled"));
                }
                if (wc.isRemoteBackupEnabled()) {
                    String url = wc.getRemotePushUrl();
                    if (url == null) {
                        log.notifyError(localized("fastback.notify.info-remote-url-missing"));
                    } else {
                        log.notify(localized("fastback.notify.info-remote-url", url));
                    }
                }
                if (wc.isShutdownBackupEnabled()) {
                    log.notify(localized("fastback.notify.info-shutdown-enabled"));
                } else {
                    log.notify(localized("fastback.notify.info-shutdown-disabled"));
                }
                final File gitDir = git.getRepository().getDirectory();
                log.notify(localized("fastback.notify.info-backup-size", getDirDisplaySize(gitDir)));
                {
                    // show the snapshot retention policy
                    final String encoded = wc.retentionPolicy();
                    if (encoded == null) {
                        log.notify(localized("fastback.notify.retention-policy-none"));
                    } else {
                        final RetentionPolicy policy = RetentionPolicyCodec.INSTANCE.
                                decodePolicy(ctx, RetentionPolicyType.getAvailable(), encoded);
                        if (policy == null) {
                            log.notify(localized("fastback.notify.retention-policy-none"));
                        } else {
                            log.notify(localized("fastback.notify.retention-policy-set"));
                            log.notify(policy.getDescription());
                        }
                    }
                }
            });
            return SUCCESS;
        });
    }
}