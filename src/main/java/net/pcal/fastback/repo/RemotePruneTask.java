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

import net.pcal.fastback.ModContext;
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.config.GitConfigKey;
import net.pcal.fastback.logging.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.repo.JGitLocalPruneTask.doPrune;

/**
 * Delete remote snapshot branches that should not be kept per the retention policy.
 *
 * @author pcal
 * @since 0.7.0
 */
class RemotePruneTask implements Callable<Collection<SnapshotId>> {

    private final ModContext ctx;
    private final Logger log;
    private final Repo repo;

    RemotePruneTask(final Repo repo,
                    final ModContext ctx,
                    final Logger log) {
        this.repo = requireNonNull(repo);
        this.ctx = requireNonNull(ctx);
        this.log = requireNonNull(log);
    }

    @Override
    public Collection<SnapshotId> call() throws IOException {
        return doPrune(repo, ctx, log,
                GitConfigKey.REMOTE_RETENTION_POLICY,
                repo::listRemoteSnapshots,
                sid -> {
                    log.info("Pruning remote snapshot " + sid.getName());
                    GitConfig conf = repo.getConfig();
                    repo.deleteRemoteBranch(conf.getString(GitConfigKey.REMOTE_NAME), sid.getBranchName());
                },
                "fastback.chat.remote-retention-policy-not-set"
        );
    }
}
