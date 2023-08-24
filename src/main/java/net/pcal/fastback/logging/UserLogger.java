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

import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.logging.UserMessage.styledLocalized;
import static net.pcal.fastback.mod.Mod.mod;

/**
 * Logging interface for messages which *might* be displayed in the UI.
 *
 * @author pcal
 * @since 0.13.0
 */
public interface UserLogger extends AutoCloseable {

    /**
     * Send a fairly important message that should be displayed in the UI a relatively prominent and durable manner.
     * Typically, this means in the chat dialog.
     */
    void message(UserMessage message);

    /**
     * Send a bit of low-level detail that is useful for indicating progress or activity but isn't of critical
     * importance.  This will typically be displayed in the HUD area.
     */
    void update(UserMessage message);

    @Override
    default void close() {
        mod().clearHudText();
    }

    default void internalError() {
        this.message(styledLocalized("fastback.chat.internal-error", ERROR));
    }

    static UserLogger forCommand(final ServerCommandSource scs) {
        return new CommandLogger(scs);
    }

    static UserLogger forShutdown() {
        return ShutdownLogger.INSTANCE;
    }

    static UserLogger forAutosave() {
        return ShutdownLogger.INSTANCE;
    }

}
