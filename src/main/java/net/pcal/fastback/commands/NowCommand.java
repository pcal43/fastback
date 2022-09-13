package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.tasks.BackupTask;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.*;
import static net.pcal.fastback.utils.GitUtils.isGitRepo;

public class NowCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final NowCommand c = new NowCommand(ctx);
        argb.then(literal("now").executes(c::now));
    }

    private final ModContext ctx;

    private NowCommand(final ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int now(CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final MinecraftServer server = cc.getSource().getServer();
            server.save(false, true, true); // suppressLogs, flush, force
            final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
            if (!isGitRepo(worldSaveDir)) {
                log.notifyError("Run '/backup enable' to enable backups.");
                return FAILURE;
            }
            final WorldConfig config = WorldConfig.load(worldSaveDir);
            if (config.isBackupEnabled()) {
                try {
                    this.ctx.setWorldSaveEnabled(server, false);
                    new BackupTask(worldSaveDir, log).run();
                } finally {
                    this.ctx.setWorldSaveEnabled(server, true);
                }
                return SUCCESS;
            } else {
                log.notifyError("Backups are disabled.  Run '/backup enable' first.");
                return FAILURE;
            }
        });
    }
}
