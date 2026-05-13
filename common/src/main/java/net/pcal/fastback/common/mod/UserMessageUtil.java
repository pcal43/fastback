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

package net.pcal.fastback.common.mod;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.pcal.fastback.common.logging.UserMessage;

import static net.minecraft.ChatFormatting.GRAY;
import static net.minecraft.ChatFormatting.GREEN;
import static net.minecraft.ChatFormatting.RED;
import static net.minecraft.ChatFormatting.YELLOW;
import static net.minecraft.network.chat.Style.EMPTY;

/**
 * Utility for converting {@link UserMessage} to Minecraft {@link Component}.
 *
 * @author pcal
 * @since 0.2.0
 */
public class UserMessageUtil {

    public static Component messageToText(final UserMessage m) {
        final MutableComponent out;
        if (m.localized() != null) {
            out = Component.translatable(
                m.localized().key(),
                messageParamsToComponentArgs(m.localized().params())
            );
        } else {
            out = Component.literal(m.raw());
        }
        switch (m.style()) {
            case ERROR -> out.setStyle(EMPTY.withColor(TextColor.fromLegacyFormat(RED)));
            case WARNING -> out.setStyle(EMPTY.withColor(TextColor.fromLegacyFormat(YELLOW)));
            case JGIT -> out.setStyle(EMPTY.withColor(TextColor.fromLegacyFormat(GRAY)));
            case NATIVE_GIT -> out.setStyle(EMPTY.withColor(TextColor.fromLegacyFormat(GREEN)));
        }
        return out;
    }

    private static Object[] messageParamsToComponentArgs(final Object[] params) {
        if (params == null) return new Object[0];

        final Object[] out = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            final Object param = params[i];
            if (param instanceof Component) {
                out[i] = param;
            } else {
                out[i] = String.valueOf(param);
            }
        }
        return out;
    }

    private UserMessageUtil() {}
}
