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
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static net.pcal.fastback.repo.SnapshotIdUtils.SnapshotIdCodec.V2;
import static net.pcal.fastback.repo.V1SnapshotIdTest.createWorldId;
import static net.pcal.fastback.repo.WorldIdUtils.generateRandomWorldId;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author pcal
 * @since 0.15.0
 */
public class V2SnapshotIdTest {

    @BeforeAll
    public static void setup() {
        SystemLogger.Singleton.register(new Log4jLogger(LogManager.getLogger("mocklogger")));
    }

    @Test
    public void testWorldIdGeneration() {
        for(int i=0; i<10000; i++) generateRandomWorldId(4);
    }

    @Test
    public void testParseBranch() throws ParseException {
        final String wid = generateRandomWorldId(4);
        final String date = "2010-05-08_01-02-03";
        final String branchName = wid + "/" + date;
        final SnapshotId sid = V2.fromBranch(branchName);
        assertEquals(date, sid.getShortName());
        assertEquals(branchName, sid.getBranchName());
        assertEquals(wid, sid.getWorldId().toString());
        final Date parsedDate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").parse(date);
        assertEquals(parsedDate, sid.getDate());
    }

    @Test
    public void testSorting() throws ParseException {
        final SnapshotId s0 = V2.fromBranch(generateRandomWorldId(4) + "/1977-09-24_01-02-03");
        final SnapshotId s1 = V2.fromBranch(generateRandomWorldId(10) + "/2010-05-08_01-02-03");
        final SnapshotId s2 = V2.fromBranch(generateRandomWorldId(100) + "/2013-10-02_01-02-03");
        List<SnapshotId> list = new ArrayList<>(List.of(s1, s2, s0));
        Collections.sort(list);
        assertEquals(List.of(s0, s1, s2), list);
    }

    @Test
    public void testSortWorldSnapshots() throws ParseException {
        final WorldId wid0 = createWorldId(generateRandomWorldId(4));
        final WorldId wid1 = createWorldId(generateRandomWorldId(5));
        final ListMultimap<WorldId, SnapshotId> sids = ArrayListMultimap.create();

        final SnapshotId s0 = V2.fromBranch(wid0 + "/1977-09-24_01-02-03");
        final SnapshotId s1 = V2.fromBranch(wid0 + "/2010-05-08_01-02-03");
        final SnapshotId s2 = V2.fromBranch(wid0 + "/2013-10-02_01-02-03");
        sids.put(wid0, s0);
        sids.put(wid0, s1);
        sids.put(wid0, s2);

        final SnapshotId s3 = V2.fromBranch(wid1 + "/1977-09-24_01-02-03");
        final SnapshotId s4 = V2.fromBranch(wid1 + "/2010-05-08_01-02-03");
        final SnapshotId s5 = V2.fromBranch(wid1 + "/2013-10-02_01-02-03");
        sids.put(wid1, s3);
        sids.put(wid1, s4);
        sids.put(wid1, s5);

        assertEquals(List.of(s0, s1, s2), List.copyOf(Repo.sortWorldSnapshots(sids, wid0)));
        assertEquals(List.of(s3, s4, s5), List.copyOf(Repo.sortWorldSnapshots(sids, wid1)));
    }


}
