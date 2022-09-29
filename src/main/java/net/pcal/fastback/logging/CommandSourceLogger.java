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
import net.pcal.fastback.ModContext;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.Message.localized;

public class CommandSourceLogger implements Logger {

    private final ServerCommandSource scs;
    private final ModContext ctx;

    public CommandSourceLogger(ModContext ctx, ServerCommandSource scs) {
        this.ctx = requireNonNull(ctx);
        this.scs = requireNonNull(scs);
    }

    @Override
    public void chat(Message message, ChatMessageType type) {
        switch(type) {
            case NORMAL -> ctx.sendFeedback(message, scs);
            case ERROR ->  ctx.sendError(message, scs);
        }
    }

    @Override
    public void internalError(String rawMessageIgnored, Throwable t) {
        ctx.sendError(localized("fastback.notify.internal-error"), scs);
    }

    @Override
    public void progressUpdate(Message message) {
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
