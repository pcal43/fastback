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

package net.pcal.fastback.repo;

import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage.UserMessageStyle;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.SystemLogger.syslog;
import static net.pcal.fastback.logging.UserMessage.styledRaw;

/**
 * Consumes strings (as from process output) and relays them to both the user's hud and the debug log.
 *
 * @author pcal
 * @since 0.13.0
 */
class HudConsumer implements Consumer<String> {

    private final UserLogger log;
    private final UserMessageStyle style;

    public HudConsumer(final UserLogger log, final UserMessageStyle style) {
        this.log = requireNonNull(log);
        this.style = requireNonNull(style);
    }

    @Override
    public void accept(String s) {
        log.hud(styledRaw(s, style));
        syslog().debug(s);
    }
}
