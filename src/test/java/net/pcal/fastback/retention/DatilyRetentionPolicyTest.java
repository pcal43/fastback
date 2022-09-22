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
import net.pcal.fastback.utils.SnapshotId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.*;

public class DatilyRetentionPolicyTest {

    @Test
    public void testDailyStrat() throws ParseException {
        final String uuid = UUID.randomUUID().toString();
        final SnapshotId LAST_YEAR = SnapshotId.fromUuidAndName(uuid, "2021-09-03_14-35-10");
        final SnapshotId YESTERDAY = SnapshotId.fromUuidAndName(uuid, "2022-09-02_14-35-10");
        final SnapshotId TODAY1 = SnapshotId.fromUuidAndName(uuid, "2022-09-03_01-11-15");
        final SnapshotId TODAY2 = SnapshotId.fromUuidAndName(uuid, "2022-09-03_11-44-19");
        final SnapshotId TODAY3 = SnapshotId.fromUuidAndName(uuid, "2022-09-03_23-11-11");
        final SnapshotId TOMORROW = SnapshotId.fromUuidAndName(uuid, "2022-09-04_01-01-01");
        Collection<SnapshotId> snapshots = Set.of(LAST_YEAR, YESTERDAY, TODAY1, TODAY2, TODAY3, TOMORROW);
        ModContext ctx = MockModContext.create();
        RetentionPolicy policy = DailyRetentionPolicyType.INSTANCE.createPolicy(ctx, Collections.emptyMap());
        Collection<SnapshotId> toPrune = policy.getSnapshotsToPrune(snapshots);
        Assertions.assertEquals(2, toPrune.size());
        Assertions.assertEquals(List.of(TODAY2, TODAY1), toPrune);
    }
}