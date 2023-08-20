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

import net.pcal.fastback.MockModContext;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.repo.SnapshotId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public class DailyRetentionPolicyTest {

    private static final long HOUR_MILLIS = 1000 * 60 * 60;
    private static final long DAY_MILLIS = HOUR_MILLIS * 24;

    @Test
    public void testDailyRetention() {
        final String uuid = UUID.randomUUID().toString();
        long now = new Date().getTime();
        final SnapshotId todayEvening = SnapshotId.create(uuid,
                new Date(now + now % DAY_MILLIS - (4 * HOUR_MILLIS)));
        final SnapshotId todayMorning = SnapshotId.create(uuid,
                new Date(todayEvening.snapshotDate().getTime() - (DAY_MILLIS / 2)));
        final SnapshotId yesterdayA = SnapshotId.create(uuid,
                new Date(todayEvening.snapshotDate().getTime() - (DAY_MILLIS) - 30000));
        final SnapshotId yesterdayB = SnapshotId.create(uuid,
                new Date(todayEvening.snapshotDate().getTime() - (DAY_MILLIS) - 20000));
        final SnapshotId yesterdayC = SnapshotId.create(uuid,
                new Date(todayEvening.snapshotDate().getTime() - (DAY_MILLIS) - 10000));
        final SnapshotId threeDaysAgoA = SnapshotId.create(uuid,
                new Date(todayEvening.snapshotDate().getTime() - (3 * DAY_MILLIS) - 30000));
        final SnapshotId threeDaysAgoB = SnapshotId.create(uuid,
                new Date(todayEvening.snapshotDate().getTime() - (3 * DAY_MILLIS) - 20000));
        final SnapshotId threeDaysAgoC = SnapshotId.create(uuid,
                new Date(todayEvening.snapshotDate().getTime() - (3 * DAY_MILLIS) - 10000));
        final SnapshotId lastWeek = SnapshotId.create(uuid,
                new Date(todayEvening.snapshotDate().getTime() - (7 * DAY_MILLIS)));
        final SnapshotId lastYearA = SnapshotId.create(uuid,
                new Date(todayEvening.snapshotDate().getTime() - (373 * DAY_MILLIS) - 30000));
        final SnapshotId lastYearB = SnapshotId.create(uuid,
                new Date(todayEvening.snapshotDate().getTime() - (373 * DAY_MILLIS) - 20000));
        final SnapshotId lastYearC = SnapshotId.create(uuid,
                new Date(todayEvening.snapshotDate().getTime() - (373 * DAY_MILLIS) - 10000));
        final int GRACE_PERIOD = 2;
        TreeSet<SnapshotId> snapshots = new TreeSet<>(Set.of(
                todayEvening, todayMorning,
                yesterdayA, yesterdayB, yesterdayC,
                threeDaysAgoA, threeDaysAgoB, threeDaysAgoC, lastWeek,
                lastYearA, lastYearB, lastYearC));
        ModContext ctx = MockModContext.create();
        RetentionPolicy policy = DailyRetentionPolicy.DailyRetentionPolicyType.INSTANCE.createPolicy(ctx,
                Map.of("gracePeriodDays", String.valueOf(GRACE_PERIOD)));
        Collection<SnapshotId> toPruneList = policy.getSnapshotsToPrune(snapshots);
        Assertions.assertEquals(List.of(threeDaysAgoB, threeDaysAgoA, lastYearB, lastYearA), toPruneList);
    }
}
