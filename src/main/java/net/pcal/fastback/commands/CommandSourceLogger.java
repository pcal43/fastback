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

package net.pcal.fastback.commands;

import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mod.ModContext;

import static java.util.Objects.requireNonNull;

class CommandSourceLogger implements UserLogger {

    private final ServerCommandSource scs;
    private final ModContext ctx;

    CommandSourceLogger(ModContext ctx, ServerCommandSource scs) {
        this.ctx = requireNonNull(ctx);
        this.scs = requireNonNull(scs);
    }

    @Override
    public void chat(UserMessage message) {
        ctx.sendChat(message, this.scs);
    }

    @Override
    public void hud(UserMessage message) {
        this.ctx.setHudText(message);
    }
}
