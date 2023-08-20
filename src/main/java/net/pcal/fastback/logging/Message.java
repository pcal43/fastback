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

public record Message(Localized localized, String raw, boolean isError) {

    public record Localized(String key, Object... params) {
    }

    public static Message localized(String key, Object... params) {
        return new Message(new Localized(key, params), null, false);
    }

    public static Message localizedError(String key, Object... params) {
        return new Message(new Localized(key, params), null, true);
    }

    public static Message raw(String text) {
        return new Message(null, requireNonNull(text), false);
    }

    public static Message rawError(String text) {
        return new Message(null, requireNonNull(text), true);
    }

}
