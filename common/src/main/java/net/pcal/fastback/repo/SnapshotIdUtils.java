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
import net.pcal.fastback.repo.WorldIdUtils.WorldIdImpl;
import org.eclipse.jgit.lib.Ref;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static net.pcal.fastback.logging.SystemLogger.syslog;

abstract class SnapshotIdUtils {

    static ListMultimap<WorldId, SnapshotId> getSnapshotsPerWorld(Iterable<Ref> refs, SnapshotIdCodec codec) {
        final ListMultimap<WorldId, SnapshotId> out = ArrayListMultimap.create();
        for (final Ref ref : refs) {
            final String branchName = BranchUtils.getBranchName(ref);
            if (branchName == null) continue;
            try {
                final SnapshotId sid = codec.fromBranch(branchName);
                if (sid != null) out.put(sid.getWorldId(), sid);
            } catch (ParseException e) {
                syslog().warn("Ignoring unexpected branch name " + branchName);
            }
        }
        return out;
    }

    enum SnapshotIdCodec {

        V2 {
            private static final String SEP = "/";

            @Override
            SnapshotId create(final WorldId wid) {
                final Date date = new Date();
                final String shortName = DATE_FORMAT.format(date);
                return new SnapshotIdImpl(wid, date, shortName, getBranchName(wid, shortName));
            }

            @Override
            SnapshotId create(final WorldId wid, String shortName) throws ParseException {
                return new SnapshotIdImpl(wid, DATE_FORMAT.parse(shortName), shortName, getBranchName(wid, shortName));
            }

            @Override
            boolean isSnapshotBranchName(WorldId wid, final String branchName) {
                return branchName.startsWith(wid + SEP);
            }

            @Override
            SnapshotId fromBranch(final String rawBranchName) throws ParseException {
                final String[] segments = rawBranchName.split(SEP);
                if (segments.length != 2) {
                    throw new ParseException("Wrong number of segments" + rawBranchName, segments.length);
                }
                final WorldId worldId = new WorldIdImpl(segments[0]);
                final Date date = DATE_FORMAT.parse(segments[1]);
                final String shortName = DATE_FORMAT.format(date);
                return new SnapshotIdImpl(worldId, date, shortName, rawBranchName);
            }

            private static String getBranchName(WorldId wid, String shortName) {
                return wid + SEP + shortName;
            }
        },


        V1 {

            private static final String PREFIX = "snapshots";
            private static final String SEP = "/";

            @Override
            SnapshotId create(WorldId wid) {
                final Date date = new Date();
                final String shortName = DATE_FORMAT.format(date);
                return new SnapshotIdImpl(wid, date, shortName, getBranchName(wid, shortName));
            }


            @Override
            SnapshotId create(WorldId wid, String shortName) throws ParseException {
                return new SnapshotIdImpl(wid, DATE_FORMAT.parse(shortName), shortName, getBranchName(wid, shortName));
            }

            @Override
            boolean isSnapshotBranchName(WorldId bid, String branchName) {
                return branchName.startsWith(PREFIX + SEP + bid);
            }

            //Committing snapshots/06628b24-118c-42ae-8cce-5d131a94c7ee/2022-09-12_23-24-50
            @Override
            SnapshotId fromBranch(String rawBranchName) throws ParseException {
                if (!rawBranchName.startsWith(PREFIX + SEP)) {
                    throw new ParseException("Not a snapshot branch " + rawBranchName, 0);
                }
                final String[] segments = rawBranchName.split(SEP);
                if (segments.length < 3) {
                    throw new ParseException("too few segments " + rawBranchName, segments.length);
                }
                final WorldId worldUuid = new WorldIdImpl(segments[1]);
                final Date date = DATE_FORMAT.parse(segments[2]);
                final String shortName = DATE_FORMAT.format(date);
                return new SnapshotIdImpl(worldUuid, date, shortName, rawBranchName);
            }

            private static String getBranchName(WorldId wid, String shortName) {
                return PREFIX + SEP + wid + SEP + shortName;
            }
        };


        static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

        abstract SnapshotId create(WorldId wid);

        abstract SnapshotId create(WorldId worldId, String shortName) throws ParseException;

        abstract SnapshotId fromBranch(String rawBranchName) throws ParseException;

        abstract boolean isSnapshotBranchName(WorldId bid, String branchName);

    }

    public record SnapshotIdImpl(WorldId worldUuid, Date date, String shortName,
                                 String branchName) implements SnapshotId {

        // ====================================================================
        // Accessors

        public String getShortName() {
            return shortName;
        }

        @Override
        public Date getDate() {
            return date;
        }

        public String getBranchName() {
            return branchName;
        }

        @Override
        public WorldId getWorldId() {
            return worldUuid;
        }

        @Override
        public int compareTo(final SnapshotId o) {
            return this.date.compareTo(o.getDate());
        }

        @Override
        public String toString() {
            return branchName;
        }
    }
}
