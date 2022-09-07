package net.pcal.fastback;

import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

import static java.util.Objects.requireNonNull;

public class BranchNameUtils {

    private static final String SNAPSHOTS_PREFIX = "snapshot/";
    private static final String DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss";

    public static String createSnapshotBranchName(final String worldUuid, final ModContext.Logger logger) {
        final String formattedDate = new SimpleDateFormat(requireNonNull(DATE_FORMAT)).format(new Date());
        return SNAPSHOTS_PREFIX + requireNonNull(worldUuid) + "/" + formattedDate;
    }

    public static String extractWorldUuid(final String fromSnapshotBranchName, final ModContext.Logger logger) {
        if (fromSnapshotBranchName.startsWith(SNAPSHOTS_PREFIX)) {
            int start = SNAPSHOTS_PREFIX.length();
            int end = fromSnapshotBranchName.indexOf('/');
            if (end == -1) {
                logger.warn("Ignore remote branch with unexpected name: " + fromSnapshotBranchName);
            }
            return fromSnapshotBranchName.substring(start, fromSnapshotBranchName.indexOf("/", start));
        }
        return null;
    }

    public static String filterOnWorldUuid(final String branchName, final String worldUuid, final ModContext.Logger logger) {
        final String prefix = SNAPSHOTS_PREFIX + worldUuid+"/";
        if (branchName.startsWith(prefix)) {
            return branchName.substring(prefix.length());
        }
        return null;
    }
}
