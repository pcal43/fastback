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

import java.time.LocalDate;
import java.util.*;

public enum StandardRetentionStrategy implements RetentionStrategy {

    DAILY() {
        @Override
        public String getConfigName() {
            return "daily";
        }

        @Override
        public Set<String> getConfigParams() {
            return Collections.emptySet();
        }

        @Override
        public String getNameKey() {
            return "fastback.retention.daily";
        }

        @Override
        public Pruner createPruner(ModContext ctx, Config gitConfig) {
            return new Pruner() {

                @Override
                public Collection<SnapshotId> getSnapshotsToPrune(Collection<SnapshotId> sortedSnapshots) {
                    final List<SnapshotId> sorted = new ArrayList<>(sortedSnapshots);
                    Collections.sort(sorted, Collections.reverseOrder());
                    final List<SnapshotId> toPrune = new ArrayList<>();
                    LocalDate previousDate = sorted.get(0).snapshotDate().toInstant().atZone(ctx.getTimeZone().toZoneId()).toLocalDate();
                    LocalDate currentDate;
                    for (int i = 1; i < sorted.size(); i++) {
                        currentDate = sorted.get(i).snapshotDate().toInstant().atZone(ctx.getTimeZone().toZoneId()).toLocalDate();
                        if (currentDate.equals(previousDate)) {
                            ctx.getLogger().info("pruning "+sorted.get(i)+ " same day as "+sorted.get(i-1));
                        //    ctx.getLogger().info(currentCal + " is the same day as "+previousCal);
                            toPrune.add(sorted.get(i));
                        } else {
                            ctx.getLogger().info("pruning "+sorted.get(i)+ " NOT same day as "+sorted.get(i-1));
                          //  ctx.getLogger().info(currentCal + " is NOT the same day as "+previousCal);
                        }
                        previousDate = currentDate;
                    }
                    return toPrune;
                }

                private static boolean isSameDay(Calendar c1, Calendar c2) {
                    return c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR) &&
                            c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
                }
            };
        }
    },
    ;


    public abstract Pruner createPruner(ModContext ctx, Config gitConfig);
}
