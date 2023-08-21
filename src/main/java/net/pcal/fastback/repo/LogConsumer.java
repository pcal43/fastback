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

import net.pcal.fastback.logging.Logger;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.Message.raw;

class LogConsumer implements Consumer<String> {

    private final Logger log;

    public LogConsumer(Logger log) {
        this.log = requireNonNull(log);
    }

    @Override
    public void accept(String s) {
        log.debug(s);
        log.hud(raw(s));
    }
}
