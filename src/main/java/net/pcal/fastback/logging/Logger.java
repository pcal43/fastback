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

public interface Logger {

    void notify(Message message);

    void notifyError(Message message);

    void progressUpdate(Message message);

    @Deprecated
    default void progressComplete(String message, int percentage) {
        progressUpdate(Message.raw(message + " " + percentage + "%"));
    }

    @Deprecated
    default void progressComplete(String message) {
        progressUpdate(Message.raw(message));
    }

    void internalError(String message, Throwable t);

    void warn(String message);

    void info(String message);

    void debug(String message);

    void debug(String message, Throwable t);
}
