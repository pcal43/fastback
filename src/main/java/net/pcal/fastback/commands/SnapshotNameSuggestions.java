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
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.SnapshotId;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;
import static net.pcal.fastback.WorldConfig.isBackupsEnabledOn;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.tasks.ListSnapshotsTask.listSnapshotsForWorldSorted;

class SnapshotNameSuggestions implements SuggestionProvider<ServerCommandSource> {

    private final ModContext ctx;

    SnapshotNameSuggestions(ModContext ctx) {
        this.ctx = requireNonNull(ctx);
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(final CommandContext<ServerCommandSource> scs,
                                                         final SuggestionsBuilder builder) {
        CompletableFuture<Suggestions> completableFuture = new CompletableFuture<>();
        this.ctx.getExecutorService().execute(() -> {
            try {
                final Logger logger = commandLogger(ctx, scs);
                final Path worldSaveDir = ctx.getWorldSaveDirectory(scs.getSource().getServer());
                if (isBackupsEnabledOn(worldSaveDir)) {
                    for (SnapshotId sid : listSnapshotsForWorldSorted(worldSaveDir, logger)) {
                        builder.suggest(sid.getName());
                    }
                    completableFuture.complete(builder.buildFuture().get());
                }
            } catch (Exception e) {
                ctx.getLogger().internalError("Failed to look up snapshot suggestions", e);
            }
        });
        return completableFuture;
    }
}
