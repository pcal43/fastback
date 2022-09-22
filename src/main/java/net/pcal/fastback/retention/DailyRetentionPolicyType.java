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

import net.pcal.fastback.ModContext;
import net.pcal.fastback.utils.SnapshotId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

enum DailyRetentionPolicyType implements RetentionPolicyType {

    INSTANCE;

    @Override
    public String getConfigKey() {
        return "daily";
    }

    @Override
    public Set<String> getConfigParams() {
        return Collections.emptySet();
    }

    @Override
    public String getCommandKey() {
        return "daily";
    }

    @Override
    public RetentionPolicy createPolicy(ModContext ctx, Properties properties) {
        return new RetentionPolicy() {
            @Override
            public Collection<SnapshotId> getSnapshotsToPrune(Collection<SnapshotId> snapshots) {
                final List<SnapshotId> sorted = new ArrayList<>(snapshots);
                Collections.sort(sorted, Collections.reverseOrder());
                final List<SnapshotId> toPrune = new ArrayList<>();
                LocalDate previousDate = sorted.get(0).snapshotDate().toInstant().atZone(ctx.getTimeZone().toZoneId()).toLocalDate();
                LocalDate currentDate;
                for (int i = 1; i < sorted.size(); i++) {
                    currentDate = sorted.get(i).snapshotDate().toInstant().atZone(ctx.getTimeZone().toZoneId()).toLocalDate();
                    if (currentDate.equals(previousDate)) {
                        ctx.getLogger().info("pruning " + sorted.get(i) + " same day as " + sorted.get(i - 1));
                        toPrune.add(sorted.get(i));
                    } else {
                        ctx.getLogger().info("pruning " + sorted.get(i) + " NOT same day as " + sorted.get(i - 1));
                    }
                    previousDate = currentDate;
                }
                return toPrune;
            }

            @Override
            public Properties getProperties() {
                return null;
            }

            @Override
            public RetentionPolicyType getType() {
                return null;
            }
        };
    }
}
