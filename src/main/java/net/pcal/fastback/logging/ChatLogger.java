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

import net.pcal.fastback.mod.ModContext;

import static java.util.Objects.requireNonNull;

@Deprecated
public class ChatLogger implements Logger {

    private final ModContext ctx;

    public ChatLogger(ModContext ctx) {
        this.ctx = requireNonNull(ctx);
    }

    @Override
    public void chat(UserMessage message) {
        ctx.sendClientChatMessage(message);
    }

    @Override
    public void hud(UserMessage message) {
    }

    @Override
    public void setForceDebugEnabled(boolean debug) {

    }

    @Override
    public void error(String message) {
        ctx.sendClientChatMessage(UserMessage.localized("fastback.chat.internal-error"));
    }

    @Override
    public void error(String message, Throwable t) {
        ctx.sendClientChatMessage(UserMessage.localized("fastback.chat.internal-error"));
    }

    @Override
    public void warn(String message) {
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
