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
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.repo.Repo;
import net.pcal.fastback.repo.SnapshotId;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.utils.Executor.ExecutionLock.NONE;

abstract class SnapshotNameSuggestions implements SuggestionProvider<ServerCommandSource> {

    static SnapshotNameSuggestions local() {
        return new SnapshotNameSuggestions() {
            @Override
            protected Iterator<SnapshotId> getSnapshotIds(Repo repo, UserLogger ulog) throws Exception {
                return repo.getLocalSnapshots().iterator();
            }
        };
    }

    static SnapshotNameSuggestions remote() {
        return new SnapshotNameSuggestions() {
            @Override
            protected Iterator<SnapshotId> getSnapshotIds(Repo repo, UserLogger ulog) throws Exception {
                return repo.getRemoteSnapshots().iterator();
            }
        };
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(final CommandContext<ServerCommandSource> cc,
                                                         final SuggestionsBuilder builder) {
        CompletableFuture<Suggestions> completableFuture = new CompletableFuture<>();
        try (final UserLogger ulog = UserLogger.forCommand(cc)) {
            gitOp(NONE, ulog, repo -> {
                final Iterator<SnapshotId> i = getSnapshotIds(repo, ulog);
                // Note to self: there's no point sorting here because the mc code (Suggestion.java) is
                // going to resort it anyway.
                while (i.hasNext()) builder.suggest(i.next().getShortName());
                completableFuture.complete(builder.buildFuture().get());
            });
        }
        return completableFuture;
    }

    abstract protected Iterator<SnapshotId> getSnapshotIds(Repo repo, UserLogger log) throws Exception;

}
