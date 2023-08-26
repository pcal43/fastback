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
import net.minecraft.text.Text;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mod.Mod;
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.retention.RetentionPolicy;
import net.pcal.fastback.retention.RetentionPolicyCodec;
import net.pcal.fastback.retention.RetentionPolicyType;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.config.FastbackConfigKey.AUTOBACK_ACTION;
import static net.pcal.fastback.config.FastbackConfigKey.AUTOBACK_WAIT_MINUTES;
import static net.pcal.fastback.config.FastbackConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.IS_NATIVE_GIT_ENABLED;
import static net.pcal.fastback.config.FastbackConfigKey.LOCAL_RETENTION_POLICY;
import static net.pcal.fastback.config.FastbackConfigKey.REMOTE_RETENTION_POLICY;
import static net.pcal.fastback.config.FastbackConfigKey.RESTORE_DIRECTORY;
import static net.pcal.fastback.config.FastbackConfigKey.SHUTDOWN_ACTION;
import static net.pcal.fastback.config.OtherConfigKey.REMOTE_PUSH_URL;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserLogger.ulog;
import static net.pcal.fastback.logging.UserMessage.raw;
import static net.pcal.fastback.mod.Mod.mod;
import static net.pcal.fastback.repo.RepoFactory.rf;
import static net.pcal.fastback.utils.EnvironmentUtils.getGitLfsVersion;
import static net.pcal.fastback.utils.EnvironmentUtils.getGitVersion;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.apache.commons.io.FileUtils.sizeOfDirectory;

enum InfoCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "info";

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> argb, Mod mod) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(COMMAND_NAME)).
                        executes(cc -> info(mod, cc.getSource()))
        );
    }

    private static int info(final Mod mod, final ServerCommandSource scs) {
        requireNonNull(mod);
        requireNonNull(scs);
        try (final UserLogger ulog = ulog(scs)) {
            try {
                final Text notInstalled = Text.translatable("fastback.values.not-installed");
                ulog.message(UserMessage.localized("fastback.chat.info-header"));
                ulog.message(UserMessage.localized("fastback.chat.info-fastback-version", mod.getModVersion()));
                final String gitVersion = getGitVersion();
                ulog.message(UserMessage.localized("fastback.chat.info-native-git-version", gitVersion != null ? gitVersion : notInstalled));
                final String gitLfsVersion = getGitLfsVersion();
                ulog.message(UserMessage.localized("fastback.chat.info-native-git-lfs-version", gitLfsVersion != null ? gitLfsVersion : notInstalled));

                if (rf().isGitRepo(mod().getWorldDirectory())) {
                    final Repo repo = rf().load(mod().getWorldDirectory());
                    final GitConfig conf = repo.getConfig();
                    ulog.message(UserMessage.localized("fastback.chat.info-uuid", repo.getWorldId()));
                    if (conf.getBoolean(IS_BACKUP_ENABLED)) {
                        ulog.message(UserMessage.localized("fastback.chat.info-local-enabled"));
                    } else {
                        ulog.message(UserMessage.localized("fastback.chat.info-local-disabled"));
                    }
                    ulog.message(UserMessage.localized("fastback.chat.info-remote-url", conf.getString(REMOTE_PUSH_URL)));
                    Path restoreDir = null;
                    try {
                        restoreDir = repo.getRestoresDir();
                    } catch (IOException ioe) {
                        syslog().error(ioe);
                    }
                    ulog.message(raw(RESTORE_DIRECTORY.getSettingName() + ": " + restoreDir));
                    final SchedulableAction shutdownAction = SchedulableAction.forConfigValue(conf.getString(SHUTDOWN_ACTION));
                    ulog.message(UserMessage.localized("fastback.chat.info-shutdown-action", getActionDisplay(shutdownAction)));
                    final SchedulableAction autobackAction = SchedulableAction.forConfigValue(conf.getString(AUTOBACK_ACTION));
                    ulog.message(UserMessage.localized("fastback.chat.info-autoback-action", getActionDisplay(autobackAction)));
                    ulog.message(UserMessage.localized("fastback.chat.info-autoback-wait", conf.getInt(AUTOBACK_WAIT_MINUTES)));

                    // FIXME? this could be implemented more efficiently
                    final long backupSize = sizeOfDirectory(repo.getDirectory());
                    final long worldSize = sizeOfDirectory(repo.getWorkTree()) - backupSize;
                    ulog.message(UserMessage.localized("fastback.chat.info-world-size", byteCountToDisplaySize(worldSize)));
                    ulog.message(UserMessage.localized("fastback.chat.info-backup-size", byteCountToDisplaySize(backupSize)));
                    showRetentionPolicy(ulog,
                            conf.getString(LOCAL_RETENTION_POLICY),
                            "fastback.chat.retention-policy-set",
                            "fastback.chat.retention-policy-none"
                    );
                    showRetentionPolicy(ulog,
                            conf.getString(REMOTE_RETENTION_POLICY),
                            "fastback.chat.remote-retention-policy-set",
                            "fastback.chat.remote-retention-policy-none"
                    );
                    ulog.message(UserMessage.localized("fastback.chat.info-native-git", conf.getBoolean(IS_NATIVE_GIT_ENABLED)));
                }
            } catch (IOException e) {
                ulog.internalError(e);
            }
        }
        return SUCCESS;
    }

    private static String getActionDisplay(SchedulableAction action) {
        return action == null ? SchedulableAction.NONE.getArgumentName() : action.getArgumentName();
    }

    private static void showRetentionPolicy(UserLogger log, String encodedPolicy, String setKey, String noneKey) {
        if (encodedPolicy == null) {
            log.message(UserMessage.localized(noneKey));
        } else {
            final RetentionPolicy policy = RetentionPolicyCodec.INSTANCE.
                    decodePolicy(RetentionPolicyType.getAvailable(), encodedPolicy);
            if (policy == null) {
                log.message(UserMessage.localized(noneKey));
            } else {
                log.message(UserMessage.localized(setKey));
                log.message(policy.getDescription());
            }
        }
    }
}
