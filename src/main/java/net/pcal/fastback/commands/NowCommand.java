package net.pcal.fastback.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.Loggr;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.tasks.BackupTask;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.GitUtils.isGitRepo;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.executeStandard;

public class NowCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> fastbackCmd, final ModContext ctx) {
        final NowCommand c = new NowCommand(ctx);
        fastbackCmd.then(literal("now").executes(c::now));
    }

    private final ModContext ctx;
    private final Loggr logger;

    private NowCommand(ModContext context) {
        this.ctx = requireNonNull(context);
        this.logger = ctx.getLogger();
    }

    private int now(CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, tali) -> {
            final MinecraftServer server = cc.getSource().getServer();
            server.save(false, true, true); // suppressLogs, flush, force
            final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
            if (!isGitRepo(worldSaveDir)) {
                tali.error("Run '/backup enable' to enable backups.");
                return FAILURE;
            }
            final WorldConfig config = WorldConfig.load(worldSaveDir);
            if (config.isBackupEnabled()) {
                try {
                    this.ctx.enableWorldSaving(server, false);
                    new BackupTask(worldSaveDir, tali, logger).run();
                } finally {
                    this.ctx.enableWorldSaving(server, true);
                }
                return SUCCESS;
            } else {
                tali.error("Backups are disabled.  Run '/backup enable' first.");
                return FAILURE;
            }
        });
    }
}
