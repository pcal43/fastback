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

package net.pcal.fastback.tasks;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;

@SuppressWarnings({"Convert2MethodRef", "FunctionalExpressionCanBeFolded"})
public class ListRemoteSnapshotsTask implements Callable<ListMultimap<String, SnapshotId>> {

    private final Git git;
    private final Logger logger;
    private final WorldConfig wc;

    public static List<SnapshotId> listRemoteSnapshotsForWorldSorted(final Git git, final Logger log) {
        final List<SnapshotId> out = new ArrayList<>();
        out.addAll(new ListRemoteSnapshotsTask(git, log).run());
        Collections.sort(out);
        return out;
    }

    public ListRemoteSnapshotsTask(Git git,  WorldConfig wc, Logger logger) {
        this.git = requireNonNull(git);
        this.logger = requireNonNull(logger);
        this.wc = requireNonNull(wc);
    }

    @Override
    public ListMultimap<String, SnapshotId> call() throws Exception {
        final ListMultimap<String, SnapshotId> snapshotsPerWorld = ArrayListMultimap.create();
        final Collection<Ref> refs = git.lsRemote().setRemote(wc.getRemoteName()).setHeads(true).call();
        for (final Ref ref : refs) {
            final SnapshotId sid = SnapshotId.fromBranchRef(ref);
            snapshotsPerWorld.put(sid.worldUuid(), sid);
        }
        return snapshotsPerWorld;
    }
}
