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

package net.pcal.fastback.mod.fabric;

import net.pcal.fastback.logging.Message;

import java.nio.file.Path;

/**
 * @author pcal
 * @since 0.1.0
 */
public class FabricServerProvider extends FabricProvider {

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public Path getSnapshotRestoreDir() {
        return null;
    }

    @Override
    public void setClientSavingScreenText(Message message) {
    }

    @Override
    public void sendClientChatMessage(Message message) {
    }

    @Override
    public void setHudText(Message message) {
    }
}
