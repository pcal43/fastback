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
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.Git;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

public class CommitAndPushTask {

    private final ModContext ctx;
    private final Logger log;
    private final Git git;

    public CommitAndPushTask(final Git git,
                             final ModContext ctx,
                             final Logger log) {
        this.git = requireNonNull(git);
        this.ctx = requireNonNull(ctx);
        this.log = requireNonNull(log);
    }

    public void run() {
        final SnapshotId newSid;
        try {
            newSid = SnapshotId.create(WorldConfig.getWorldUuid(git));
        } catch (IOException e) {
            this.log.internalError("uuid lookup failed", e);
            return;
        }
        new CommitTask(git, ctx, log, newSid).run();
        new PushTask(git, ctx, log, newSid).run();
    }
}
