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

import net.minecraft.text.Text;
import net.pcal.fastback.Message;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class CompositeLogger implements Logger {

    private final Iterable<Logger> delegates;

    public static Logger of(Logger... loggers) {
        return new CompositeLogger(List.of(loggers));
    }

    public CompositeLogger(Iterable<Logger> delegates) {
        this.delegates = requireNonNull(delegates);
    }

    @Override
    public void notify(Message message) {
        this.delegates.forEach(d -> d.notify(message));
    }

    @Override
    public void notifyError(Message message) {
        this.delegates.forEach(d -> d.notifyError(message));
    }

    @Override
    public void notify(Text message) {
        this.delegates.forEach(d -> d.notify(message));
    }

    @Override
    public void progressComplete(String message, int percentage) {
        this.delegates.forEach(d -> d.progressComplete(message, percentage));
    }

    @Override
    public void progressComplete(String message) {
        this.delegates.forEach(d -> d.progressComplete(message));
    }

    @Override
    public void notifyError(Text message) {
        this.delegates.forEach(d -> d.notifyError(message));
    }

    @Override
    public void internalError(String message, Throwable t) {
        this.delegates.forEach(d -> d.internalError(message, t));
    }

    @Override
    public void warn(String message) {
        this.delegates.forEach(d -> d.warn(message));
    }

    @Override
    public void info(String message) {
        this.delegates.forEach(d -> d.info(message));
    }

    @Override
    public void debug(String message) {
        this.delegates.forEach(d -> d.debug(message));
    }

    @Override
    public void debug(String message, Throwable t) {
        this.delegates.forEach(d -> d.debug(message, t));
    }
}
