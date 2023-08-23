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

package net.pcal.fastback.mod;

/**
 * Framework-agnostic lifecycle logic.
 *
 * @author pcal
 * @since 0.0.1
 */
public interface LifecycleListener {

    /**
     * Must be called early in initialization of either a client or server.
     */
    void onInitialize();

    /**
     * Must be called when a world is starting (in either a dedicated or client-embedded server).
     */
    void onWorldStart();

    /**
     * Must be called when a world is stopping (in either a dedicated or client-embedded server).
     */
    void onWorldStop();
}
