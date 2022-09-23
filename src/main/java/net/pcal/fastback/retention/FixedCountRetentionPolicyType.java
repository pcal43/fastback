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

package net.pcal.fastback.retention;


import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.logging.Message;
import net.pcal.fastback.utils.SnapshotId;

import java.util.*;

/**
 * Retention policy that keeps only the most-recent snapshot of each day.  Provides for a grace period
 * during which all snapshots are retained.
 *
 * @author pcal
 * @since 0.1.5
 */
public enum FixedCountRetentionPolicyType implements RetentionPolicyType {

    INSTANCE;

    private static final String COUNT = "count";
    private static final int COUNT_DEFAULT = 10;

    @Override
    public String getName() {
        return "count";
    }

    @Override
    public List<Parameter> getParameters() {
        return List.of(new Parameter("count", IntegerArgumentType.integer(1)));
    }

    @Override
    public RetentionPolicy createPolicy(final ModContext ctx, final Map<String, String> config) {
        return new RetentionPolicy() {

            @Override
            public Message getDescription() {
                return null;
            }

            @Override
            public Collection<SnapshotId> getSnapshotsToPrune(Collection<SnapshotId> fromSnapshots) {
                int count = COUNT_DEFAULT;
                if (config != null && config.containsKey(COUNT)) {
                    try {
                        count = Integer.parseInt(config.get(COUNT));
                    } catch (NumberFormatException nfe) {
                        ctx.getLogger().internalError("invalid count " + config.get(COUNT), nfe);
                    }
                }
                final List<SnapshotId> sorted = new ArrayList<>(fromSnapshots);
                sorted.sort(Collections.reverseOrder());
                if (sorted.size() > count) {
                    return sorted.subList(count - 1, sorted.size() - 1);
                } else {
                    return Collections.emptySet();
                }
            }
        };
    }
}
