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
import net.pcal.fastback.retention.RetentionPolicy;
import net.pcal.fastback.retention.RetentionPolicyCodec;
import net.pcal.fastback.utils.SnapshotId;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.nio.file.Path;
import java.util.Collection;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.*;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.tasks.ListSnapshotsTask.listSnapshotsForWorldSorted;

/**
 * Command to prune all snapshots that are not to be retained per the retention policy.
 *
 * @author pcal
 * @since 0.1.5
 */
public class PruneCommand {

    private static final String COMMAND_NAME = "prune";

    public static void register(LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final PruneCommand c = new PruneCommand(ctx);
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(c::prune)
        );
    }

    private final ModContext ctx;

    private PruneCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int prune(final CommandContext<ServerCommandSource> cc) {
        return executeStandardNew(this.ctx, cc, (git, wc, log) -> {
            this.ctx.getExecutorService().execute(() -> {
                final String policyConfig = wc.retentionPolicy();
                if (policyConfig == null) {
                    log.notifyError(localized("fastback.notify.prune-no-default"));
                    return;
                }
                final RetentionPolicy policy = RetentionPolicyCodec.INSTANCE.decodePolicy
                        (ctx, ctx.getAvailableRetentionPolicyTypes(), policyConfig);
                if (policy == null) {
                    log.notifyError(localized("fastback.notify.retention-policy-not-set"));
                    return;
                }
                final MinecraftServer server = cc.getSource().getServer();
                final Path worldSaveDir = this.ctx.getWorldSaveDirectory(server);
                Collection<SnapshotId> toPrune = policy.getSnapshotsToPrune(listSnapshotsForWorldSorted(worldSaveDir, ctx.getLogger()));
                int pruned = 0;
                for (final SnapshotId sid : toPrune) {
                    log.notify(localized("fastback.notify.prune-pruning", sid.getName()));
                    try {
                        git.branchDelete().setForce(true).setBranchNames(new String[]{sid.getBranchName()}).call();
                        pruned++;
                    } catch (final GitAPIException e) {
                        log.internalError("failed to prune branch for " + sid, e);
                    }
                }
                log.notify(localized("fastback.notify.prune-done", pruned));
                if (pruned > 0) {
                    log.notify(localized("fastback.notify.prune-suggest-gc"));
                }
            });
            return SUCCESS;
        });
    }
}
