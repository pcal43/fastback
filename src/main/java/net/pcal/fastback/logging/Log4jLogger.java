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

import static java.util.Objects.requireNonNull;

public class Log4jLogger implements Logger {

    private final org.apache.logging.log4j.Logger log4j;

    public Log4jLogger(org.apache.logging.log4j.Logger log4j) {
        this.log4j = requireNonNull(log4j);
    }

    @Override
    public void notify(Message message) {
    }

    @Override
    public void notifyError(Message message) {
    }

    @Override
    public void progressComplete(String message, int percent) {
        this.log4j.info("[PROGRESS " + message + " " + percent);
    }

    @Override
    public void progressComplete(String message) {
        this.log4j.info("[PROGRESS-COMPLETE] " + message);
    }

    @Override
    public void internalError(String message, Throwable t) {
        this.log4j.error(message, t);
    }

    @Override
    public void warn(String message) {
        this.log4j.warn(message);
    }

    @Override
    public void info(String message) {
        this.log4j.info(message);
    }

    @Override
    public void debug(String message) {
        this.log4j.debug(message);
    }

    @Override
    public void debug(String message, Throwable t) {
        this.log4j.debug(message, t);
    }

}
