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
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.repo.RepoFactory;
import net.pcal.fastback.utils.Executor;

import java.nio.file.Path;
import java.time.Duration;

import static net.pcal.fastback.commands.SchedulableAction.NONE;
import static net.pcal.fastback.commands.SchedulableAction.forConfigValue;
import static net.pcal.fastback.config.GitConfigKey.AUTOBACK_ACTION;
import static net.pcal.fastback.config.GitConfigKey.AUTOBACK_WAIT_MINUTES;
import static net.pcal.fastback.config.GitConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.logging.SystemLogger.syslog;

class AutosaveListener implements Runnable {

    private final Mod mod;
    private long lastBackupTime = System.currentTimeMillis();

    AutosaveListener(Mod mod) {
        this.mod = mod;
    }

    @Override
    public void run() {
        //TODO implement indicator
        // final Logger screenLogger = CompositeLogger.of(ctx.getLogger(), new SaveScreenLogger(ctx));
        mod.getExecutor().execute(Executor.ExecutionLock.WRITE, new HudLogger(mod), () -> {
            RepoFactory rf = RepoFactory.get();
            final Path worldSaveDir = mod.getWorldDirectory();
            if (!rf.isGitRepo(worldSaveDir)) return;
            try (final Repo repo = rf.load(worldSaveDir, mod)) {
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
                autobackAction.getTask(repo, new HudLogger(mod));
                lastBackupTime = System.currentTimeMillis();
            } catch (Exception e) {
                syslog().error("auto-backup failed.", e);
            } finally {
                mod.clearHudText();
            }
        });
    }
}
