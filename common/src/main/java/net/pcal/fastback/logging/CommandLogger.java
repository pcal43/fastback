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
import static net.pcal.fastback.mod.Mod.mod;

import net.minecraft.commands.CommandSourceStack;

/**
 * Handles messages in the context of a command executed by the user in the console or chat box.
 *
 * @author pcal
 * @since 0.15.0
 */
class CommandLogger implements UserLogger {

    private final CommandSourceStack scs;

    CommandLogger(final CommandSourceStack scs) {
        this.scs = requireNonNull(scs);
    }

    @Override
    public void message(final UserMessage message) {
        mod().sendChat(message, this.scs);
    }

    @Override
    public void update(final UserMessage message) {
        mod().setHudText(message);
    }
}
