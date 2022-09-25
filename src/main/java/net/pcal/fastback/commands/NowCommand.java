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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.tasks.BackupTask;

import java.nio.file.Path;

import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.executeStandardNew;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.utils.GitUtils.isGitRepo;

public class NowCommand {

    private static final String COMMAND_NAME = "now";

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(cc->now(ctx, cc.getSource()))
        );
    }

    public static int now(final ModContext ctx, final ServerCommandSource scs) {
        return executeStandardNew(ctx, scs, (gitc, wc, log) -> {
            final MinecraftServer server = scs.getServer();
            final Path worldSaveDir = ctx.getWorldSaveDirectory(server);
            if (!isGitRepo(worldSaveDir)) {
                log.notifyError(localized("fastback.notify.not-enabled"));
                return FAILURE;
            }
            final WorldConfig config = WorldConfig.load(worldSaveDir);
            if (config.isBackupEnabled()) {
                ctx.getExecutorService().execute(() -> {
                    log.info("Saving before backup");
                    server.saveAll(false, true, true); // suppressLogs, flush, force
                    log.info("Starting backup");
                    new BackupTask(ctx, worldSaveDir, log).run();
                });
                return SUCCESS;
            } else {
                log.notifyError(localized("fastback.notify.not-enabled"));
                return FAILURE;
            }
        });
    }
}
