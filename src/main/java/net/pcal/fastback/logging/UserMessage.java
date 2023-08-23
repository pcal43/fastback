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
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.ERROR;
import static net.pcal.fastback.logging.UserMessage.UserMessageStyle.NORMAL;

/**
 * Abstract representation of a message to be displayed on the user's screen.  The message may or may
 * not be localizable.
 *
 * @author pcal
 */
public record UserMessage(LocalizedUserMessage localized, String raw, UserMessageStyle style) {

    public enum UserMessageStyle {
        NORMAL,
        WARNING,
        ERROR,
        JGIT,
        NATIVE_GIT,
        BROADCAST,
    }

    public record LocalizedUserMessage(String key, Object... params) {

        @Override
        public String toString() {
            return this.key + " " + (this.params != null ? params : "[]");
        }
    }

    public static UserMessage localized(String key, Object... params) {
        return styledLocalized(key, NORMAL, params);
    }

    public static UserMessage styledLocalized(String key, UserMessageStyle style, Object... params) {
        return new UserMessage(new LocalizedUserMessage(key, params), null, style);
    }

    public static UserMessage raw(String text) {
        return styledRaw(text, NORMAL);
    }

    public static UserMessage styledRaw(String text, UserMessageStyle style) {
        return new UserMessage(null, requireNonNull(text), style);
    }

    @Override
    public String toString() {
        return this.raw != null ? raw : this.localized != null ? this.localized.toString() : null;
    }

    // ======================================================================
    // Deprecated stuff

    @Deprecated
    public static UserMessage localizedError(String key, Object... params) {
        return styledLocalized(key, ERROR, params);
    }

    @Deprecated
    public static UserMessage rawError(String text) {
        return styledRaw(text, ERROR);
    }

}
