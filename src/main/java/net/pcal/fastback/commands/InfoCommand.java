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
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.mod.ModContext;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.retention.RetentionPolicy;
import net.pcal.fastback.retention.RetentionPolicyCodec;
import net.pcal.fastback.retention.RetentionPolicyType;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.mod.ModContext.ExecutionLock.NONE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.config.GitConfigKey.AUTOBACK_ACTION;
import static net.pcal.fastback.config.GitConfigKey.AUTOBACK_WAIT_MINUTES;
import static net.pcal.fastback.config.GitConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.config.GitConfigKey.LOCAL_RETENTION_POLICY;
import static net.pcal.fastback.config.GitConfigKey.REMOTE_PUSH_URL;
import static net.pcal.fastback.config.GitConfigKey.REMOTE_RETENTION_POLICY;
import static net.pcal.fastback.config.GitConfigKey.SHUTDOWN_ACTION;
import static net.pcal.fastback.logging.Message.localized;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.apache.commons.io.FileUtils.sizeOfDirectory;

enum InfoCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "info";

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> argb, ModContext ctx) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(cc -> info(ctx, cc.getSource()))
        );
    }

    private static int info(final ModContext ctx, final ServerCommandSource scs) {
        requireNonNull(ctx);
        requireNonNull(scs);
        final Logger log = commandLogger(ctx, scs);
        gitOp(ctx, NONE, log, repo -> {
            final GitConfig c = repo.getConfig();
            log.chat(localized("fastback.chat.info-fastback-version", ctx.getModVersion()));
            log.chat(localized("fastback.chat.info-uuid", repo.getWorldUuid()));
            if (c.getBoolean(IS_BACKUP_ENABLED)) {
                log.chat(localized("fastback.chat.info-local-enabled"));
            } else {
                log.chat(localized("fastback.chat.info-local-disabled"));
            }
            log.chat(localized("fastback.chat.info-remote-url", c.getString(REMOTE_PUSH_URL)));
            final SchedulableAction shutdownAction = SchedulableAction.forConfigValue(c.getString(SHUTDOWN_ACTION));
            log.chat(localized("fastback.chat.info-shutdown-action", getActionDisplay(shutdownAction)));
            final SchedulableAction autobackAction = SchedulableAction.forConfigValue(c.getString(AUTOBACK_ACTION));
            log.chat(localized("fastback.chat.info-autoback-action", getActionDisplay(autobackAction)));
            log.chat(localized("fastback.chat.info-autoback-wait", c.getInt(AUTOBACK_WAIT_MINUTES)));

            // FIXME? this could be implemented more efficiently
            final long backupSize = sizeOfDirectory(repo.getDirectory());
            final long worldSize = sizeOfDirectory(repo.getWorkTree()) - backupSize;
            log.chat(localized("fastback.chat.info-world-size", byteCountToDisplaySize(worldSize)));
            log.chat(localized("fastback.chat.info-backup-size", byteCountToDisplaySize(backupSize)));
            showRetentionPolicy(ctx, log,
                    c.getString(LOCAL_RETENTION_POLICY),
                    "fastback.chat.retention-policy-set",
                    "fastback.chat.retention-policy-none"
            );
            showRetentionPolicy(ctx, log,
                    c.getString(REMOTE_RETENTION_POLICY),
                    "fastback.chat.remote-retention-policy-set",
                    "fastback.chat.remote-retention-policy-none"
            );
        });
        return SUCCESS;
    }

    private static String getActionDisplay(SchedulableAction action) {
        return action == null ? SchedulableAction.NONE.getArgumentName() : action.getArgumentName();
    }

    private static void showRetentionPolicy(ModContext ctx, Logger log, String encodedPolicy, String setKey, String noneKey) {
        if (encodedPolicy == null) {
            log.chat(localized(noneKey));
        } else {
            final RetentionPolicy policy = RetentionPolicyCodec.INSTANCE.
                    decodePolicy(RetentionPolicyType.getAvailable(), encodedPolicy);
            if (policy == null) {
                log.chat(localized(noneKey));
            } else {
                log.chat(localized(setKey));
                log.chat(policy.getDescription());
            }
        }
    }
}
