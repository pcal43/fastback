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
import java.util.Date;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.gitback.commands.Commands.*;

//
// We basically want to
//   git reflog expire --expire-unreachable=now --all
//   git gc --prune=now
// But jgit gc seems to have bugs or I'm just using it wrong.  Disabling this command
// until I have time to figure it out.  https://www.eclipse.org/lists/jgit-dev/msg03782.html
//
public class GcCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final GcCommand c = new GcCommand(ctx);
        argb.then(literal("gc").executes(c::now));
    }

    private final ModContext ctx;

    private GcCommand(final ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int now(CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            ctx.getExecutorService().execute(() -> {
                final ProgressMonitor pm =
                        new IncrementalProgressMonitor(new LoggingProgressMonitor(log), 100);
                try (final Git git = Git.open(wc.worldSaveDir().toFile())) {
                    log.notify("Collecting garbage in local backup...");
                    log.info("Stats before gc:");
                    log.info(""+git.gc().getStatistics());
                    git.gc().setExpire(new Date()).setAggressive(false).setProgressMonitor(pm).call();
                    log.notify("Garbage collection complete.");
                    log.info("Stats after gc:");
                    log.info(""+git.gc().getStatistics());

                } catch (IOException | GitAPIException e) {
                    log.internalError("Failed to gc", e);
                }
            });
            return SUCCESS;
        });
    }
}
