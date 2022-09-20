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

package net.pcal.fastback.utils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import net.pcal.fastback.logging.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public record SnapshotId(String worldUuid, Date snapshotDate) implements Comparable<SnapshotId> {

    private static final String PREFIX = "snapshots";
    private static final String SEP = "/";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public static ListMultimap<String, SnapshotId> getSnapshotsPerWorld(Iterable<Ref> refs, Logger logger) throws GitAPIException {
        final ListMultimap<String, SnapshotId> out = ArrayListMultimap.create();
        for (final Ref ref : refs) {
            final String branchName = GitUtils.getBranchName(ref);
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

    //Committing snapshots/06628b24-118c-42ae-8cce-5d131a94c7ee/2022-09-12_23-24-50
    public static SnapshotId fromBranch(String rawBranchName) throws ParseException {
        if (!rawBranchName.startsWith(PREFIX + SEP)) return null;
        final String[] segments = rawBranchName.split(SEP);
        if (segments.length < 3) throw new ParseException("too few segments " + rawBranchName, segments.length);
        final String worldUuid = segments[1];
        final Date date = DATE_FORMAT.parse(segments[2]);
        return new SnapshotId(worldUuid, date);
    }

    public static SnapshotId create(String worldUuid) {
        return new SnapshotId(worldUuid, new Date());
    }

    public static SnapshotId fromUuidAndName(String worldUuid, String snapshoDate) throws ParseException {
        return new SnapshotId(worldUuid, DATE_FORMAT.parse(snapshoDate));
    }

    public String getName() {
        return DATE_FORMAT.format(this.snapshotDate);
    }

    public String getBranchName() {
        final String formattedDate = DATE_FORMAT.format(this.snapshotDate);
        return PREFIX + "/" + this.worldUuid + "/" + formattedDate;
    }

    @Override
    public int compareTo(@NotNull SnapshotId o) {
        return this.snapshotDate.compareTo(o.snapshotDate);
    }

    @Override
    public String toString() {
        return getBranchName();
    }
}
