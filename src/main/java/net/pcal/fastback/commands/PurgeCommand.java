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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.Git;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.executeStandard;

public class PurgeCommand {


    private static final String SNAPSHOT_ARG = "snapshot";

    public static void register(LiteralArgumentBuilder<ServerCommandSource> argb, ModContext ctx) {
        final PurgeCommand rc = new PurgeCommand(ctx);
        argb.then(literal("purge").
                then(argument(SNAPSHOT_ARG, StringArgumentType.string()).
                        suggests(new SnapshotNameSuggestions(ctx)).
                        executes(rc::execute)));
    }

    private final ModContext ctx;

    private PurgeCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int execute(CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, log) -> {
            final String snapshotName = cc.getLastChild().getArgument(SNAPSHOT_ARG, String.class);
            final SnapshotId sid = SnapshotId.fromUuidAndName(wc.worldUuid(), snapshotName);
            final String branchName = sid.getBranchName();
            final MinecraftServer server = cc.getSource().getServer();
            final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
            this.ctx.getExecutorService().execute(() -> {
                try (final Git git = Git.open(worldSaveDir.toFile())) {
                    git.branchDelete().setForce(true).setBranchNames(branchName).call();
                    log.notify("Deleted backup snapshot " + snapshotName);
                } catch (final Exception e) {
                    log.internalError("Failed to purge", e);
                }
            });
            return SUCCESS;
        });
    }
}
