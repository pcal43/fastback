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

import net.minecraft.text.Text;
import net.pcal.fastback.Message;
import net.pcal.fastback.ModContext;

import static java.util.Objects.requireNonNull;
import static net.minecraft.text.Text.literal;
import static net.minecraft.text.Text.translatable;

public class SaveScreenLogger implements Logger {

    private final ModContext ctx;

    public SaveScreenLogger(final ModContext ctx) {
        this.ctx = requireNonNull(ctx);
    }

    @Override
    public void progressComplete(String message, int percentage) {
        Text text = null;
        if (message.contains("Finding sources")) {
            text = translatable("fastback.savescreen.remote-preparing", percentage);
        } else if (message.contains("Writing objects")) {
            text = translatable("fastback.savescreen.remote-uploading", percentage);
        }
        if (text == null) text = literal(message + " " + percentage + "%");
        this.ctx.setSavingScreenText(text);
    }

    @Override
    public void progressComplete(String message) {
        Text text = null;
        if (message.contains("Writing objects")) {
            text = translatable("fastback.savescreen.remote-done");
        }
        if (text == null) text = literal(message);
        this.ctx.setSavingScreenText(text);
    }

    @Override
    public void notify(Message message) {
        this.ctx.setSavingScreenText(message);

    }

    @Override
    public void notifyError(Message message) {
        this.ctx.setSavingScreenText(message);
    }

    @Override
    public void notify(Text message) {
        this.ctx.setSavingScreenText(message);
    }

    @Override
    public void notifyError(Text message) {
        this.ctx.setSavingScreenText(message);
    }

    @Override
    public void internalError(String message, Throwable t) {
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
