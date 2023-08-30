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

import net.pcal.fastback.logging.Log4jLogger;
import net.pcal.fastback.logging.SystemLogger;
import net.pcal.fastback.repo.SnapshotId;
import net.pcal.fastback.repo.WorldId;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.*;

import static net.pcal.fastback.repo.V1SnapshotIdTest.createWorldId;
import static net.pcal.fastback.repo.V1SnapshotIdTest.v1sid;

public class DailyRetentionPolicyTest {

    private static final long HOUR_MILLIS = 1000 * 60 * 60;
    private static final long DAY_MILLIS = HOUR_MILLIS * 24;

    @BeforeAll
    public static void setup() {
        SystemLogger.Singleton.register(new Log4jLogger(LogManager.getLogger("mocklogger")));
    }

    @Test
    public void testDailyRetention() throws ParseException {
        final WorldId uuid = createWorldId(UUID.randomUUID().toString());
        long now = new Date().getTime();
        final SnapshotId todayEvening = v1sid(uuid,  new Date(now + now % DAY_MILLIS - (4 * HOUR_MILLIS)));
        final SnapshotId todayMorning = v1sid(uuid,  new Date(todayEvening.getDate().getTime() - (DAY_MILLIS / 2)));
        final SnapshotId yesterdayA = v1sid(uuid,    new Date(todayEvening.getDate().getTime() - (DAY_MILLIS) - 30000));
        final SnapshotId yesterdayB = v1sid(uuid,    new Date(todayEvening.getDate().getTime() - (DAY_MILLIS) - 20000));
        final SnapshotId yesterdayC = v1sid(uuid,    new Date(todayEvening.getDate().getTime() - (DAY_MILLIS) - 10000));
        final SnapshotId threeDaysAgoA = v1sid(uuid, new Date(todayEvening.getDate().getTime() - (3 * DAY_MILLIS) - 30000));
        final SnapshotId threeDaysAgoB = v1sid(uuid, new Date(todayEvening.getDate().getTime() - (3 * DAY_MILLIS) - 20000));
        final SnapshotId threeDaysAgoC = v1sid(uuid, new Date(todayEvening.getDate().getTime() - (3 * DAY_MILLIS) - 10000));
        final SnapshotId lastWeek = v1sid(uuid,      new Date(todayEvening.getDate().getTime() - (7 * DAY_MILLIS)));
        final SnapshotId lastYearA = v1sid(uuid,     new Date(todayEvening.getDate().getTime() - (373 * DAY_MILLIS) - 30000));
        final SnapshotId lastYearB = v1sid(uuid,     new Date(todayEvening.getDate().getTime() - (373 * DAY_MILLIS) - 20000));
        final SnapshotId lastYearC = v1sid(uuid,     new Date(todayEvening.getDate().getTime() - (373 * DAY_MILLIS) - 10000));
        final int GRACE_PERIOD = 2;
        TreeSet<SnapshotId> snapshots = new TreeSet<>(Set.of(
                todayEvening, todayMorning,
                yesterdayA, yesterdayB, yesterdayC,
                threeDaysAgoA, threeDaysAgoB, threeDaysAgoC, lastWeek,
                lastYearA, lastYearB, lastYearC));

        RetentionPolicy policy = DailyRetentionPolicy.DailyRetentionPolicyType.INSTANCE.createPolicy(
                Map.of("gracePeriodDays", String.valueOf(GRACE_PERIOD)));
        Collection<SnapshotId> toPruneList = policy.getSnapshotsToPrune(snapshots);
        Assertions.assertEquals(List.of(threeDaysAgoB, threeDaysAgoA, lastYearB, lastYearA), toPruneList);
    }

}
