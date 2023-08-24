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

package net.pcal.fastback.logging;

import java.io.IOException;

/**
 * Singleton logger instance that writes to the serverside console.
 *
 * @author pcal
 * @since 0.12.0
 */
public interface SystemLogger {

    static SystemLogger syslog() {
        return Singleton.INSTANCE;
    }

    void setForceDebugEnabled(boolean debug);

    void error(String message);

    void error(String message, Throwable t);

    default void error(Throwable e) {
        this.error(e.getMessage(), e);
    }

    void warn(String message);

    void info(String message);

    void debug(String message);

    void debug(String message, Throwable t);

    default void debug(Throwable t) {
        this.debug(t.getMessage(), t);
    }

    class Singleton {
        private static SystemLogger INSTANCE = null;

        public static void register(SystemLogger logger) {
            Singleton.INSTANCE = logger;
        }
    }
}
