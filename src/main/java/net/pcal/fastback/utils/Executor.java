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

/**
 * Thin wrapper around an ExecutorService.  Use this to do things in separate threads.
 *
 * @author pcal
 * @since 0.2.0
 */
public interface Executor {

    static Executor executor() {
        return Singleton.INSTANCE;
    }

    // TODO kill UserLogger param
    boolean execute(final ExecutionLock lock, final UserLogger ulog, final Runnable runnable);

    int getActiveCount();

    void start();

    void stop();

    enum ExecutionLock {
        NONE,
        WRITE_CONFIG,
        WRITE,
    }

    class Singleton {
        private static final Executor INSTANCE = new ExecutorImpl();
    }
}
