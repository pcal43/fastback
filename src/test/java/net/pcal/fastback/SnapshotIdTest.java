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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import net.pcal.fastback.utils.SnapshotId;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SnapshotIdTest {

    @Test
    public void testParseBranch() throws ParseException {
        final String uuid = UUID.randomUUID().toString();
        final String date = "2010-05-08_01-02-03";
        final String branchName = "snapshots/" + uuid + "/" + date;
        final SnapshotId sid = SnapshotId.fromBranch(branchName);
        assertEquals(date, sid.getName());
        assertEquals(branchName, sid.getBranchName());
        assertEquals(uuid, sid.worldUuid());
        final Date parsedDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").parse(date);
        assertEquals(parsedDate, sid.snapshotDate());
    }

    @Test
    public void testSorting() throws ParseException {
        final SnapshotId s0 = SnapshotId.fromBranch("snapshots/" + UUID.randomUUID() + "/1977-09-24_01-02-03");
        final SnapshotId s1 = SnapshotId.fromBranch("snapshots/" + UUID.randomUUID() + "/2010-05-08_01-02-03");
        final SnapshotId s2 = SnapshotId.fromBranch("snapshots/" + UUID.randomUUID() + "/2013-10-02_01-02-03");
        List<SnapshotId> list = new ArrayList<>(List.of(s1, s2, s0));
        Collections.sort(list);
        assertEquals(List.of(s0, s1, s2), list);
    }

    @Test
    public void testSortWorldSnapshots() throws ParseException {
        final String uuid0 = UUID.randomUUID().toString();
        final String uuid1 = UUID.randomUUID().toString();
        final ListMultimap<String, SnapshotId> sids = ArrayListMultimap.create();

        final SnapshotId s0 = SnapshotId.fromBranch("snapshots/" + uuid0 + "/1977-09-24_01-02-03");
        final SnapshotId s1 = SnapshotId.fromBranch("snapshots/" + uuid0 + "/2010-05-08_01-02-03");
        final SnapshotId s2 = SnapshotId.fromBranch("snapshots/" + uuid0 + "/2013-10-02_01-02-03");
        sids.put(uuid0, s0);
        sids.put(uuid0, s1);
        sids.put(uuid0, s2);

        final SnapshotId s3 = SnapshotId.fromBranch("snapshots/" + uuid1 + "/1977-09-24_01-02-03");
        final SnapshotId s4 = SnapshotId.fromBranch("snapshots/" + uuid1 + "/2010-05-08_01-02-03");
        final SnapshotId s5 = SnapshotId.fromBranch("snapshots/" + uuid1 + "/2013-10-02_01-02-03");
        sids.put(uuid1, s3);
        sids.put(uuid1, s4);
        sids.put(uuid1, s5);

        assertEquals(List.of(s0, s1, s2), List.copyOf(SnapshotId.sortWorldSnapshots(sids, uuid0)));
        assertEquals(List.of(s3, s4, s5), List.copyOf(SnapshotId.sortWorldSnapshots(sids, uuid1)));
    }

}
