package net.pcal.gitback.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.gitback.ModContext;
import net.pcal.gitback.logging.Logger;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;
import static net.pcal.gitback.WorldConfig.isBackupsEnabledOn;
import static net.pcal.gitback.commands.Commands.commandLogger;
import static net.pcal.gitback.tasks.ListSnapshotsTask.listSnapshotsForWorldSorted;

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
                    for(String s : listSnapshotsForWorldSorted(worldSaveDir, logger)) {
                        builder.suggest(s);
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
