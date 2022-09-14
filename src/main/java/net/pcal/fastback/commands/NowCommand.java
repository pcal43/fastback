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
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.executeStandard;
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
