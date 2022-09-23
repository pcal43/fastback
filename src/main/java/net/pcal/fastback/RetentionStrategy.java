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

import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.lib.Config;

import java.util.Collection;
import java.util.Set;

public interface RetentionStrategy {

    String getConfigName();

    Set<String> getConfigParams();

    String getNameKey();

    Pruner createPruner(ModContext ctx, Config gitConfig);

    public interface Pruner {
        Collection<SnapshotId> getSnapshotsToPrune(Collection<SnapshotId> fromSnapshots);
    }
}
