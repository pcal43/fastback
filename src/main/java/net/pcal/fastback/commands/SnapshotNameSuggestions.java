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

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.mod.Mod.mod;
import static net.pcal.fastback.repo.SnapshotId.sortWorldSnapshots;
import static net.pcal.fastback.utils.Executor.ExecutionLock.NONE;

abstract class SnapshotNameSuggestions implements SuggestionProvider<ServerCommandSource> {

    static SnapshotNameSuggestions local() {
        return new SnapshotNameSuggestions() {

            @Override
            protected Collection<SnapshotId> getSnapshotIds(Repo repo, UserLogger ulog) throws Exception {
                return sortWorldSnapshots(repo.listSnapshots(), repo.getWorldId());
            }
        };
    }

    static SnapshotNameSuggestions remote() {
        return new SnapshotNameSuggestions() {

            @Override
            protected Collection<SnapshotId> getSnapshotIds(Repo repo, UserLogger ulog) throws Exception {
                return sortWorldSnapshots(repo.listRemoteSnapshots(), repo.getWorldId());
            }
        };
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(final CommandContext<ServerCommandSource> cc,
                                                         final SuggestionsBuilder builder) {
        CompletableFuture<Suggestions> completableFuture = new CompletableFuture<>();
        final UserLogger ulog = commandLogger(mod(), cc.getSource());
        gitOp(mod(), NONE, ulog, repo -> {
            for (final SnapshotId sid : this.getSnapshotIds(repo, ulog)) {
                builder.suggest(sid.getName());
            }
            completableFuture.complete(builder.buildFuture().get());
        });
        return completableFuture;
    }

    abstract protected Collection<SnapshotId> getSnapshotIds(Repo repo, UserLogger log) throws Exception;

}
