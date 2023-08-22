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

import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.commands.SchedulableAction;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.logging.ConsoleLogger;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.logging.UserMessage.UserMessageStyle;
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.repo.RepoFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.file.Files.createTempDirectory;
import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.commands.SchedulableAction.NONE;
import static net.pcal.fastback.commands.SchedulableAction.forConfigValue;
import static net.pcal.fastback.config.GitConfigKey.AUTOBACK_ACTION;
import static net.pcal.fastback.config.GitConfigKey.AUTOBACK_WAIT_MINUTES;
import static net.pcal.fastback.config.GitConfigKey.IS_BACKUP_ENABLED;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.logging.UserMessage.styledLocalized;

public class ModContext {

    private final FrameworkServiceProvider spi;
    private ExecutorService executor = null;
    private Path tempRestoresDirectory = null;

    public static ModContext create(FrameworkServiceProvider spi) {
        return new ModContext(spi);
    }

    private ModContext(FrameworkServiceProvider spi) {
        this.spi = requireNonNull(spi);
        spi.setAutoSaveListener(new AutosaveListener());
    }

    class AutosaveListener implements Runnable {

        private long lastBackupTime = System.currentTimeMillis();

        @Override
        public void run() {
            //TODO implement indicator
            // final Logger screenLogger = CompositeLogger.of(ctx.getLogger(), new SaveScreenLogger(ctx));
            execute(ExecutionLock.WRITE, ConsoleLogger.get(), () -> {
                RepoFactory rf = RepoFactory.get();
                final Path worldSaveDir = getWorldDirectory();
                if (!rf.isGitRepo(worldSaveDir)) return;
                try (final Repo repo = rf.load(worldSaveDir, ModContext.this, ConsoleLogger.get())) {
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
                    autobackAction.getTask(repo);
                    lastBackupTime = System.currentTimeMillis();
                } catch (Exception e) {
                    syslog().error("auto-backup failed.", e);
                }
            });
        }
    }

    public enum ExecutionLock {
        NONE,
        WRITE_CONFIG,
        WRITE,
    }

    private Future<?> exclusiveFuture = null;

    //FIXME break this out, probably into a singleton
    public boolean execute(ExecutionLock lock, Logger log, Runnable runnable) {
        if (this.executor == null) throw new IllegalStateException("Executor not started");
        switch (lock) {
            case NONE:
            case WRITE_CONFIG: // revisit this
                this.executor.submit(runnable);
                return true;
            case WRITE:
                if (this.exclusiveFuture != null && !this.exclusiveFuture.isDone()) {
                    log.chat(styledLocalized("fastback.chat.thread-busy", ERROR));
                    return false;
                } else {
                    log.debug("executing " + runnable);
                    this.exclusiveFuture = this.executor.submit(runnable);
                    return true;
                }
            default:
                throw new IllegalStateException();
        }
    }

    public void startExecutor() {
        this.executor = new ThreadPoolExecutor(0, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    public void stopExecutor() {
        shutdownExecutor(this.executor);
        this.executor = null;
    }

    public String getCommandName() {
        return "backup"; // TODO i18n?
    }

    public Path getRestoresDir() throws IOException {
        Path restoreDir = this.spi.getSnapshotRestoreDir();
        if (restoreDir != null) return restoreDir;
        if (tempRestoresDirectory == null) {
            tempRestoresDirectory = createTempDirectory(getModId() + "-restore");
        }
        return tempRestoresDirectory;
    }

    // PASSTHROUGH IMPLEMENTATIONS

    String getModId() {
        return this.spi.getModId();
    }

    public String getModVersion() {
        return this.spi.getModVersion();
    }

    public void setWorldSaveEnabled(boolean enabled) {
        this.spi.setWorldSaveEnabled(enabled);
    }

    public boolean isClient() {
        return spi.isClient();
    }

    public boolean isServerStopping() {
        return spi.isServerStopping();
    }

    public void setSavingScreenText(UserMessage message) {
        this.spi.setClientSavingScreenText(message);
    }

    public void sendClientChatMessage(UserMessage message) {
        this.spi.sendClientChatMessage(message);
    }

    public void sendFeedback(UserMessage message, ServerCommandSource scs) {
        this.spi.sendFeedback(message, scs);
    }

    public void sendError(UserMessage message, ServerCommandSource scs) {
        this.spi.sendError(message, scs);
    }

    public void renderBackupIndicator(UserMessage message) {
        this.spi.setHudText(message);
    }

    public Path getWorldDirectory() {
        return this.spi.getWorldDirectory();
    }

    public String getWorldName() {
        return this.spi.getWorldName();
    }

    // TODO make these configurable via properties

    @Deprecated
    public boolean isExperimentalCommandsEnabled() {
        return false;
    }

    @Deprecated
    public boolean isFileRemoteBare() {
        return true;
    }

    public int getDefaultPermLevel() {
        return spi.isClient() ? 0 : 4;
    }

    public void saveWorld() {
        this.spi.saveWorld();
    }


    /**
     * Lifted straight from the docs:
     * https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html
     */
    private static void shutdownExecutor(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(5, TimeUnit.MINUTES)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(5, TimeUnit.MINUTES))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
