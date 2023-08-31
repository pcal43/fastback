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
import net.pcal.fastback.repo.V1SnapshotIdTest;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.function.Function;

public class GFSRetentionPolicyTest {

    @BeforeAll
    public static void setup() {
        SystemLogger.Singleton.register(new Log4jLogger(LogManager.getLogger("mocklogger")));
    }

    @Test
    public void testGFSRetention() throws ParseException {

        final LocalDate now = LocalDate.of(2023, 2, 23); // this is a wednesday
        final List<SnapshotId> expectPruned = new ArrayList<>();
        Function<SnapshotId, SnapshotId> pruned = sid -> {
            expectPruned.add(sid);
            return sid;
        };

        TreeSet<SnapshotId> snapshots = new TreeSet<>(Set.of(
                sid(2023, 2, 23, 9), sid(2023, 2, 23, 8), sid(2023, 2, 23, 7), // keep everything from today
                sid(2023, 2, 22, 9), sid(2023, 2, 22, 8), sid(2023, 2, 22, 7), // and yesterday, too
                // these are on unique days in the past week, should be kept
                sid(2023, 2, 16, 9), sid(2023, 2, 17, 9), sid(2023, 2, 18, 9),
                // this one is earlier in the day on the 18th, should be pruned
                pruned.apply(sid(2023, 2, 18, 8)),
                // keep only the newest one from the previous week
                sid(2023, 2, 11, 9), pruned.apply(sid(2023, 2, 7, 8)), pruned.apply(sid(2023, 2, 6, 8)),
                // same thing the week before that
                sid(2023, 2, 4, 9), pruned.apply(sid(2023, 1, 31, 8)), pruned.apply(sid(2023, 1, 30, 8)),
                // and then we get into the previous month - pruning is more aggressive
                sid(2023, 1, 17), pruned.apply(sid(2023, 1, 9)), pruned.apply(sid(2023, 1, 2)), pruned.apply(sid(2023, 1, 1, 8)),
                // previous month, same deal...
                sid(2022, 12, 31), pruned.apply(sid(2022, 12, 15)), pruned.apply(sid(2022, 12, 1)),
                // and so on
                sid(2022, 11, 4), pruned.apply(sid(2022, 11, 3)), pruned.apply(sid(2022, 11, 2))

        ));
        RetentionPolicy policy = GFSRetentionPolicy.GFSRetentionPolicyType.INSTANCE.createPolicy(Collections.emptyMap());
        ((GFSRetentionPolicy) policy).nowSupplier = () -> now;
        Collection<SnapshotId> toPruneList = policy.getSnapshotsToPrune(snapshots);
        Assertions.assertEquals(expectPruned, toPruneList);
    }

    private static SnapshotId sid(int year, int month, int day) throws ParseException {
        return sid(year, month, day, 11);
    }

    private static SnapshotId sid(int year, int month, int day, int hour) throws ParseException {
        Date date = Date.from(ZonedDateTime.of(LocalDate.of(year, month, day).atTime(hour, 0), TimeZone.getDefault().toZoneId()).toInstant());
        return V1SnapshotIdTest.v1sid("3552efde-b34d-11ed-afa1-0242ac120002", date);
    }
}
