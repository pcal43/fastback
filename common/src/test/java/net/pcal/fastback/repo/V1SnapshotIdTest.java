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

package net.pcal.fastback.repo;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import net.pcal.fastback.logging.Log4jLogger;
import net.pcal.fastback.logging.SystemLogger;
import net.pcal.fastback.repo.SnapshotIdUtils.SnapshotIdCodec;
import net.pcal.fastback.repo.WorldIdUtils.WorldIdImpl;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static net.pcal.fastback.repo.SnapshotIdUtils.SnapshotIdCodec.V1;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author pcal
 * @since 0.4.0
 */
public class V1SnapshotIdTest {

    @BeforeAll
    public static void setup() {
        SystemLogger.Singleton.register(new Log4jLogger(LogManager.getLogger("mocklogger")));
    }

    @Test
    public void testParseBranch() throws ParseException {
        final String uuid = UUID.randomUUID().toString();
        final String date = "2010-05-08_01-02-03";
        final String branchName = "snapshots/" + uuid + "/" + date;
        final SnapshotId sid = V1.fromBranch(branchName);
        assertEquals(date, sid.getShortName());
        assertEquals(branchName, sid.getBranchName());
        assertEquals(uuid, sid.getWorldId().toString());
        final Date parsedDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").parse(date);
        assertEquals(parsedDate, sid.getDate());
    }

    @Test
    public void testSorting() throws ParseException {
        final SnapshotId s0 = V1.fromBranch("snapshots/" + UUID.randomUUID() + "/1977-09-24_01-02-03");
        final SnapshotId s1 = V1.fromBranch("snapshots/" + UUID.randomUUID() + "/2010-05-08_01-02-03");
        final SnapshotId s2 = V1.fromBranch("snapshots/" + UUID.randomUUID() + "/2013-10-02_01-02-03");
        List<SnapshotId> list = new ArrayList<>(List.of(s1, s2, s0));
        Collections.sort(list);
        assertEquals(List.of(s0, s1, s2), list);
    }

    @Test
    public void testSortWorldSnapshots() throws ParseException {
        final WorldId uuid0 = new WorldIdImpl(UUID.randomUUID().toString());
        final WorldId uuid1 = new WorldIdImpl(UUID.randomUUID().toString());
        final ListMultimap<WorldId, SnapshotId> sids = ArrayListMultimap.create();

        final SnapshotId s0 = V1.fromBranch("snapshots/" + uuid0 + "/1977-09-24_01-02-03");
        final SnapshotId s1 = V1.fromBranch("snapshots/" + uuid0 + "/2010-05-08_01-02-03");
        final SnapshotId s2 = V1.fromBranch("snapshots/" + uuid0 + "/2013-10-02_01-02-03");
        sids.put(uuid0, s0);
        sids.put(uuid0, s1);
        sids.put(uuid0, s2);

        final SnapshotId s3 = V1.fromBranch("snapshots/" + uuid1 + "/1977-09-24_01-02-03");
        final SnapshotId s4 = V1.fromBranch("snapshots/" + uuid1 + "/2010-05-08_01-02-03");
        final SnapshotId s5 = V1.fromBranch("snapshots/" + uuid1 + "/2013-10-02_01-02-03");
        sids.put(uuid1, s3);
        sids.put(uuid1, s4);
        sids.put(uuid1, s5);

        assertEquals(List.of(s0, s1, s2), sorted(sids.get(uuid0)));
        assertEquals(List.of(s3, s4, s5), sorted(sids.get(uuid1)));
    }

    private static List<SnapshotId> sorted(Collection<SnapshotId> sids) {
        List<SnapshotId> out = new ArrayList<>(sids);
        Collections.sort(out);
        return out;
    }

    // so other tests can get at it
    public static SnapshotId v1sid(WorldId wid, Date date) throws ParseException {
        return V1.create(wid, SnapshotIdCodec.DATE_FORMAT.format(date));
    }

    public static SnapshotId v1sid(String wid, Date date) throws ParseException {
        return V1.create(createWorldId(wid), SnapshotIdCodec.DATE_FORMAT.format(date));
    }

    public static WorldId createWorldId(String wid) {
        return new WorldIdImpl(wid);
    }
}
