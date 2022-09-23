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

package net.pcal.fastback;

import static java.util.Objects.requireNonNull;

public enum SchedulableActions {

    LOCAL("local"),
    FULL("now"),
    GC("gc"),
    PRUNE("prune");
//    PRUNE_REMOTE("prune-remote"),
//    RECONCILE("reconcile");

    private final String command;

    SchedulableActions(String command) {
        this.command = requireNonNull(command);
    }

}
