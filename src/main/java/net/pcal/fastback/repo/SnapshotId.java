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
import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.lib.Ref;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * A globally-unique identifier for a single backup snapshot.
 *
 * @author pcal
 */
public record SnapshotId(String worldUuid, Date snapshotDate) implements Comparable<SnapshotId> {

    // ====================================================================
    // Constants

    private static final String PREFIX = "snapshots";
    private static final String SEP = "/";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    @Deprecated
    public static String getBranchName(Ref fromBranchRef) {
        final String REFS_HEADS = "refs/heads/";
        final String name = fromBranchRef.getName();
        if (name.startsWith(REFS_HEADS)) {
            return name.substring(REFS_HEADS.length());
        } else {
            return null;
        }
    }

    // ====================================================================
    // Accessors

    public String getName() {
        return DATE_FORMAT.format(this.snapshotDate);
    }

    public String getBranchName() {
        final String formattedDate = DATE_FORMAT.format(this.snapshotDate);
        return PREFIX + SEP + this.worldUuid + SEP + formattedDate;
    }

    @Override
    public int compareTo(final SnapshotId o) {
        return this.snapshotDate.compareTo(o.snapshotDate);
    }

    @Override
    public String toString() {
        return getBranchName();
    }

    // ====================================================================
    // Factories and utils

    static ListMultimap<String, SnapshotId> getSnapshotsPerWorld(Iterable<Ref> refs, Logger logger) {
        final ListMultimap<String, SnapshotId> out = ArrayListMultimap.create();
        for (final Ref ref : refs) {
            final String branchName = getBranchName(ref);
            if (branchName == null) continue;
            try {
                final SnapshotId sid = fromBranch(branchName);
                if (sid != null) out.put(sid.worldUuid(), sid);
            } catch (ParseException e) {
                logger.warn("Ignoring unexpected branch name " + branchName);
            }
        }
        return out;
    }

    static SnapshotId fromBranchRef(Ref ref) throws ParseException {
        final String REFS_HEADS = "refs/heads/";
        String name = ref.getName();
        if (!name.startsWith(REFS_HEADS)) {
            throw new ParseException("Not a branch ref "+ref, -1);
        } else {
            name = name.substring(REFS_HEADS.length());
        }
        return fromBranch(name);
    }

    //Committing snapshots/06628b24-118c-42ae-8cce-5d131a94c7ee/2022-09-12_23-24-50
    static SnapshotId fromBranch(String rawBranchName) throws ParseException {
        if (!rawBranchName.startsWith(PREFIX + SEP)) {
            throw new ParseException("Not a snapshot branch "+rawBranchName, 0);
        }
        final String[] segments = rawBranchName.split(SEP);
        if (segments.length < 3) throw new ParseException("too few segments " + rawBranchName, segments.length);
        final String worldUuid = segments[1];
        final Date date = DATE_FORMAT.parse(segments[2]);
        return new SnapshotId(worldUuid, date);
    }

    static SnapshotId create(String worldUuid) {
        return new SnapshotId(worldUuid, new Date());
    }


    static boolean isSnapshotBranchName(String branchName) {
        return branchName.startsWith(PREFIX + SEP);
    }

    public static SnapshotId create(String worldUuid, Date date) {
        return new SnapshotId(worldUuid, date);
    }

    public static SnapshotId fromUuidAndName(String worldUuid, String snapshotDate) throws ParseException {
        return new SnapshotId(worldUuid, DATE_FORMAT.parse(snapshotDate));
    }

    /**
     * Extract the sids that apply to the given world and return them in a sorted list.
     */
    public static NavigableSet<SnapshotId> sortWorldSnapshots(ListMultimap<String, SnapshotId> snapshotsPerWorld, String worldUuid) {
        return new TreeSet<>(snapshotsPerWorld.get(worldUuid));
    }

}
