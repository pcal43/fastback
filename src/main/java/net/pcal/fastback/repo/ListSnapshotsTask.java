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
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;

@SuppressWarnings({"Convert2MethodRef", "FunctionalExpressionCanBeFolded"})
class ListSnapshotsTask implements Callable<ListMultimap<String, SnapshotId>> {


    private final JGitSupplier<Collection<Ref>> refProvider;
    private final Logger logger;

    ListSnapshotsTask(JGitSupplier<Collection<Ref>> refProvider, Logger logger) {
        this.logger = requireNonNull(logger);
        this.refProvider = requireNonNull(refProvider);
    }

    @Override
    public ListMultimap<String, SnapshotId> call() throws GitAPIException, IOException {
        final ListMultimap<String, SnapshotId> snapshotsPerWorld = ArrayListMultimap.create();
        final Collection<Ref> refs = this.refProvider.get();
        for (final Ref ref : refs) {
            final SnapshotId sid;
            try {
                sid = requireNonNull(SnapshotId.fromBranchRef(ref));
            } catch (ParseException pe) {
                logger.warn("Ignoring unrecognized branch " + ref.getName());
                continue;
            }
            snapshotsPerWorld.put(sid.worldUuid(), sid);
        }
        return snapshotsPerWorld;
    }

}
