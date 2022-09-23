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

import net.pcal.fastback.Localized;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.utils.SnapshotId;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

import static net.pcal.fastback.Localized.localized;

/**
 * Retention policy that keeps only the most-recent snapshot of each day.  Provides for a grace period
 * during which all snapshots are retained.
 *
 * @author pcal
 * @since 0.1.5
 */
public enum DailyRetentionPolicyType implements RetentionPolicyType {

    INSTANCE;

    private static final String GRACE_PERIOD_DAYS = "gracePeriodDays";
    private static final int DEFAULT_GRACE_PERIOD_DAYS = 3;

    @Override
    public String getName() {
        return "daily";
    }

    @Override
    public List<Parameter> getParameters() {
        return List.of(new Parameter("gracePeriodDays", Integer.class));
    }

    @Override
    public RetentionPolicy createPolicy(final ModContext ctx, final Map<String, String> config) {
        int gracePeriodTemp = DEFAULT_GRACE_PERIOD_DAYS;
        if (config != null && config.containsKey(GRACE_PERIOD_DAYS)) {
            try {
                gracePeriodTemp = Integer.parseInt(config.get(GRACE_PERIOD_DAYS));
            } catch(NumberFormatException nfe) {
                ctx.getLogger().internalError("invalid grace period "+config.get(GRACE_PERIOD_DAYS), nfe);
            }
        }
        final int gracePeriod = gracePeriodTemp;

        return new RetentionPolicy() {

            @Override
            public Localized getDescription() {
                return localized("fastback.policy.daily", gracePeriod);
            }

            @Override
            public Collection<SnapshotId> getSnapshotsToPrune(Collection<SnapshotId> snapshots) {
                final LocalDate today = LocalDate.now(ctx.getTimeZone().toZoneId());
                final LocalDate gracePeriodStart = today.minus(Period.ofDays(gracePeriod));
                final List<SnapshotId> sorted = new ArrayList<>(snapshots);
                Collections.sort(sorted, Collections.reverseOrder());
                final List<SnapshotId> toPrune = new ArrayList<>();
                LocalDate previousDate = sorted.get(0).snapshotDate().toInstant().atZone(ctx.getTimeZone().toZoneId()).toLocalDate();
                LocalDate currentDate;
                for (int i = 1; i < sorted.size(); i++) {
                    currentDate = sorted.get(i).snapshotDate().toInstant().atZone(ctx.getTimeZone().toZoneId()).toLocalDate();
                    if (currentDate.isAfter(gracePeriodStart)) {
                        ctx.getLogger().info("retaining " +sorted.get(i) + " because still in the grace period");
                        continue;
                    }
                    if (currentDate.equals(previousDate)) {
                        ctx.getLogger().info("pruning " + sorted.get(i) + " same day as " + sorted.get(i - 1));
                        toPrune.add(sorted.get(i));
                    } else {
                        ctx.getLogger().info("retaining " + sorted.get(i) + " NOT same day as " + sorted.get(i - 1));
                    }
                    previousDate = currentDate;
                }
                return toPrune;
            }
        };
    }
}
