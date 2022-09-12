package net.pcal.fastback.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.logging.Logger;

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
                    for (String s : listSnapshotsForWorldSorted(worldSaveDir, logger)) {
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
