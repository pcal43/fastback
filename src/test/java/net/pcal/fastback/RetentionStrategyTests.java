package net.pcal.fastback;

import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.lib.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class RetentionStrategyTests {

    @Test
    public void testDailyStrat() throws ParseException {
        String uuid = "783abc5d-9776-4478-960e-09925ae4d113";

        final SnapshotId LAST_YEAR = SnapshotId.fromUuidAndName(uuid, "2021-09-03_14-35-10");
        final SnapshotId YESTERDAY = SnapshotId.fromUuidAndName(uuid, "2022-09-02_14-35-10");
        final SnapshotId TODAY1 = SnapshotId.fromUuidAndName(uuid, "2022-09-03_01-11-15");
        final SnapshotId TODAY2 = SnapshotId.fromUuidAndName(uuid, "2022-09-03_11-44-19");
        final SnapshotId TODAY3 = SnapshotId.fromUuidAndName(uuid, "2022-09-03_23-11-11");
        final SnapshotId TOMORROW = SnapshotId.fromUuidAndName(uuid, "2022-09-04_01-01-01");
        Collection<SnapshotId> snapshots = Set.of(LAST_YEAR, YESTERDAY, TODAY1, TODAY2, TODAY3, TOMORROW);
        ModContext ctx = MockModContext.create();
        RetentionStrategy.Pruner pruner = StandardRetentionStrategy.DAILY.createPruner(ctx, new Config());
        Collection<SnapshotId> toPrune = pruner.getSnapshotsToPrune(snapshots);
        Assertions.assertEquals(2, toPrune.size());
        Assertions.assertEquals(List.of(TODAY2, TODAY1), toPrune);
    }


}