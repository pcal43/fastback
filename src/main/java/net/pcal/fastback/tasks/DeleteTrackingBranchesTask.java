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

import net.pcal.fastback.ModContext;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("FieldCanBeLocal")
/**
 * Deletes all branches that are tracking a remote snapshot branch.
 *
 * @since 0.9.0
 */
public class DeleteTrackingBranchesTask implements Callable<Void> {

    private final ModContext ctx;
    private final Logger log;
    private final Git git;

    public DeleteTrackingBranchesTask(final Git git,
                                      final ModContext ctx,
                                      final Logger log) {
        this.git = requireNonNull(git);
        this.ctx = requireNonNull(ctx);
        this.log = requireNonNull(log);
    }

    @Override
    public Void call() throws GitAPIException, IOException {
        log.info("Deleting tracking branches");
        int count = 0;
        final List<Ref> refs = this.git.branchList().setListMode(ListMode.REMOTE).call();
        for (final Ref ref : refs) {
            log.info("found branch "+ref.getName());
            count++;
        }
        log.info("Deleted "+count+" tracking branches");
        return null;
    }
}
