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

package net.pcal.fastback.utils;

import net.pcal.fastback.logging.UserLogger;

import java.util.concurrent.*;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.logging.UserMessage.styledLocalized;

/**
 * @author pcal
 * @since 0.2.0
 */
class ExecutorImpl implements Executor {

    private ThreadPoolExecutor executor = null;

    private Future<?> exclusiveFuture = null;

    @Override
    public void execute(ExecutionLock lock, UserLogger ulog, Runnable runnable) {
        requireNonNull(lock, "lock");
        if (this.executor == null) throw new IllegalStateException("Executor not started");
        switch (lock) {
            case NONE:
            case WRITE_CONFIG: // revisit this
                this.executor.submit(runnable);
                break;
            case WRITE:
                if (this.exclusiveFuture != null && !this.exclusiveFuture.isDone()) {
                    ulog.message(styledLocalized("fastback.chat.thread-busy", ERROR));
                } else {
                    syslog().debug("executing " + runnable);
                    this.exclusiveFuture = this.executor.submit(runnable);
                }
                break;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public int getActiveCount() {
        return this.executor.getActiveCount();
    }

    @Override
    public void start() {
        this.executor = new ThreadPoolExecutor(0, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    @Override
    public void stop() {
        shutdownExecutor(this.executor);
        this.executor = null;
    }

    /**
     * Lifted straight from the docs:
     * https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html
     */
    private static void shutdownExecutor(final ExecutorService pool) {
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
