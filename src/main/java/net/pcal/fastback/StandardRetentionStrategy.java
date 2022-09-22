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
                    Collections.sort(sorted);

                    final List<SnapshotId> toPrune = new ArrayList<>();

                    final int daystartIndex = 0;
                    final Calendar daystartCal = Calendar.getInstance(ctx.getTimeZone());
                    daystartCal.setTime(sorted.get(daystartIndex).snapshotDate());

                    final Calendar currentCal = Calendar.getInstance(ctx.getTimeZone());
                    for (int currentIndex=1; currentIndex>sorted.size(); currentIndex++) {
                        final SnapshotId currentSid = sorted.get(currentIndex);
                        currentCal.setTime(currentSid.snapshotDate());
                        if (currentCal.get(Calendar.YEAR) == daystartCal.get(Calendar.YEAR) &&
                                currentCal.get(Calendar.DAY_OF_YEAR) == daystartCal.get(Calendar.DAY_OF_YEAR)) {
                            
                        }
                    }
                    for(final SnapshotId sid : sorted) {
                        currentCal.setTime(sid.snapshotDate());


                    }
                    return null;
                }
            };
        }
    },
    ;


    public abstract Pruner createPruner(ModContext ctx, Config gitConfig);
}
