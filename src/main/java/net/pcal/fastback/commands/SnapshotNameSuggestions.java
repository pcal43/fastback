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
package net.pcal.fastback.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.Git;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.ModContext.ExecutionLock.NONE;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.tasks.ListSnapshotsTask.listRemoteSnapshots;
import static net.pcal.fastback.tasks.ListSnapshotsTask.listSnapshots;
import static net.pcal.fastback.utils.SnapshotId.sortWorldSnapshots;

abstract class SnapshotNameSuggestions implements SuggestionProvider<ServerCommandSource> {

    static SnapshotNameSuggestions local(final ModContext ctx) {
        return new SnapshotNameSuggestions(ctx) {

            @Override
            protected Collection<SnapshotId> getSnapshotIds(Git git, Logger log) throws Exception {
                final WorldConfig wc = WorldConfig.load(git);
                return sortWorldSnapshots(listSnapshots(git, log), wc.worldUuid());
            }
        };
    }

    static SnapshotNameSuggestions remote(final ModContext ctx) {
        return new SnapshotNameSuggestions(ctx) {

            @Override
            protected Collection<SnapshotId> getSnapshotIds(Git git, Logger log) throws Exception {
                final WorldConfig wc = WorldConfig.load(git);
                return sortWorldSnapshots(listRemoteSnapshots(git, wc, log), wc.worldUuid());
            }
        };
    }

    private final ModContext ctx;

    private SnapshotNameSuggestions(ModContext ctx) {
        this.ctx = requireNonNull(ctx);
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(final CommandContext<ServerCommandSource> cc,
                                                         final SuggestionsBuilder builder) {
        CompletableFuture<Suggestions> completableFuture = new CompletableFuture<>();
        final Logger log = commandLogger(ctx, cc.getSource());
        gitOp(ctx, NONE, log, git -> {
            for (final SnapshotId sid : this.getSnapshotIds(git, log)) {
                builder.suggest(sid.getName());
            }
            completableFuture.complete(builder.buildFuture().get());
        });
        return completableFuture;
    }

    abstract protected Collection<SnapshotId> getSnapshotIds(Git git, Logger log) throws Exception;

}
