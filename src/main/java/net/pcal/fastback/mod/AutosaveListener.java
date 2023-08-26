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

package net.pcal.fastback.mod;

import net.pcal.fastback.commands.SchedulableAction;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.repo.RepoFactory;
import net.pcal.fastback.utils.Executor;

import java.nio.file.Path;
import java.time.Duration;

import static net.pcal.fastback.commands.SchedulableAction.NONE;
import static net.pcal.fastback.commands.SchedulableAction.forConfigValue;
import static net.pcal.fastback.config.FastbackConfigKey.AUTOBACK_ACTION;
import static net.pcal.fastback.config.FastbackConfigKey.AUTOBACK_WAIT_MINUTES;
import static net.pcal.fastback.config.FastbackConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.mod.Mod.mod;
import static net.pcal.fastback.utils.Executor.executor;

/**
 * @author pcal
 * @since 0.2.0
 */
class AutosaveListener implements Runnable {

    private long lastBackupTime = System.currentTimeMillis();

    @Override
    public void run() {
        try (final UserLogger ulog = UserLogger.forAutosave()) {
            executor().execute(Executor.ExecutionLock.WRITE, ulog, () -> {
                try {
                    final RepoFactory rf = RepoFactory.rf();
                    final Path worldSaveDir = mod().getWorldDirectory();
                    if (!rf.isGitRepo(worldSaveDir)) return;
                    try (final Repo repo = rf.load(worldSaveDir)) {
                        final GitConfig config = repo.getConfig();
                        if (!config.getBoolean(IS_BACKUP_ENABLED)) return;
                        final SchedulableAction autobackAction = forConfigValue(config, AUTOBACK_ACTION);
                        if (autobackAction == null || autobackAction == NONE) return;
                        final Duration waitTime = Duration.ofMinutes(config.getInt(AUTOBACK_WAIT_MINUTES));
                        final Duration timeRemaining = waitTime.
                                minus(Duration.ofMillis(System.currentTimeMillis() - lastBackupTime));
                        if (!timeRemaining.isZero() && !timeRemaining.isNegative()) {
                            syslog().debug("Skipping auto-backup until at least " +
                                    (timeRemaining.toSeconds() / 60) + " more minutes have elapsed.");
                            return;
                        }
                        syslog().info("Starting auto-backup");
                        autobackAction.getTask(repo, ulog).call();
                    }
                    lastBackupTime = System.currentTimeMillis();
                } catch (Exception e) {
                    syslog().error("auto-backup failed.", e);
                }
            });
        }
    }

}
