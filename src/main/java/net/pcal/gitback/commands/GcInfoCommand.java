package net.pcal.gitback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.gitback.ModContext;
import net.pcal.gitback.logging.IncrementalProgressMonitor;
import net.pcal.gitback.logging.LoggingProgressMonitor;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.gitback.commands.Commands.SUCCESS;
import static net.pcal.gitback.commands.Commands.executeStandard;

public class GcInfoCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final GcInfoCommand c = new GcInfoCommand(ctx);
        argb.then(literal("gc-info").executes(c::now));
    }

    private final ModContext ctx;

    private GcInfoCommand(final ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int now(CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            ctx.getExecutorService().execute(() -> {
                final ProgressMonitor pm =
                        new IncrementalProgressMonitor(new LoggingProgressMonitor(log), 100);
                try (final Git git = Git.open(wc.worldSaveDir().toFile())) {
                    final Properties stats = git.gc().getStatistics();
                    List<String> props = List.copyOf(stats.stringPropertyNames());
                    Collections.sort(props);
                    props.forEach(p -> log.notify(p + ": " + stats.get(p)));
                } catch (GitAPIException | IOException e) {
                    log.internalError("error gathering gc info", e);
                }
            });
            return SUCCESS;
        });
    }
}
