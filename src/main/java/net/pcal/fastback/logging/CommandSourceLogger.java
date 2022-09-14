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

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static java.util.Objects.requireNonNull;
import static net.minecraft.text.Text.translatable;

public class CommandSourceLogger implements Logger {

    private final ServerCommandSource scs;

    public CommandSourceLogger(ServerCommandSource scs) {
        this.scs = requireNonNull(scs);
    }

    @Override
    public void notify(Text message) {
        scs.sendFeedback(message, false);
    }

    @Override
    public void notifyError(Text message) {
        scs.sendError(message);
    }

    @Override
    public void internalError(String message, Throwable t) {
        scs.sendError(translatable("fastback.notify.internal-error"));
    }

    @Override
    public void warn(String message) {
    }

    @Override
    public void progressComplete(String message) {
    }

    @Override
    public void progressComplete(String message, int percentage) {
    }

    @Override
    public void info(String message) {
    }

    @Override
    public void debug(String message) {
    }

    @Override
    public void debug(String message, Throwable t) {
    }
}
