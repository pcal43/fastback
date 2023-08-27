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

import net.pcal.fastback.repo.SnapshotIdUtils.SnapshotIdCodec;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.logging.SystemLogger.syslog;


/**
 * @author pcal
 */
abstract class BranchUtils {

    /**
     * Get the snapshots for this repo.  Snapshot branches for worlds other than the Repo's are ignored.
     */
    static Set<SnapshotId> listSnapshots(RepoImpl repo, JGitSupplier<Collection<Ref>> refProvider) throws GitAPIException, IOException {
        final Collection<Ref> refs = refProvider.get();
        final SnapshotIdCodec codec = repo.getSidCodec();
        final Set<SnapshotId> out = new HashSet<>();
        for (final Ref ref : refs) {
            String branchName = getBranchName(ref);
            if (repo.getSidCodec().isSnapshotBranchName(repo.getWorldId(), branchName)) {
                final SnapshotId sid;
                try {
                    sid = requireNonNull(codec.fromBranch(branchName));
                } catch (ParseException pe) {
                    syslog().error("Unexpected parse error, ignoring branch "+branchName, pe);
                    continue;
                }
                if (sid.getWorldId().equals(repo.getWorldId())) {
                    out.add(sid);
                } else {
                    syslog().debug("Ignoring branch from other world "+branchName);
                }
            } else {
                syslog().debug("Ignoring unrecognized branch "+branchName);
            }
        }
        return out;
    }

    static String getBranchName(Ref fromBranchRef) {
       final String REFS_HEADS = "refs/heads/";
       final String name = fromBranchRef.getName();
       if (name.startsWith(REFS_HEADS)) {
           return name.substring(REFS_HEADS.length());
       } else {
           return null;
       }
   }

}
