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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Message;
import org.eclipse.jgit.lib.StoredConfig;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.executeStandardNew;
import static net.pcal.fastback.commands.Commands.subcommandPermission;

public class SetShutdownActionCommand {

    private static final String COMMAND_NAME = "set-shutdown-action";
    private static final String ARGUMENT = "action";

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).then(
                                argument(ARGUMENT, StringArgumentType.greedyString()).
                                        executes(cc->execute(ctx, cc)))
        );
    }

    public static int execute(final ModContext ctx, final CommandContext<ServerCommandSource> cc) throws CommandSyntaxException {
        return executeStandardNew(ctx, cc.getSource(), (git, wc, log) -> {
            final String actionRaw = cc.getArgument(ARGUMENT, String.class);
            final SchedulableAction action = SchedulableAction.getForConfigKey(actionRaw);
            if (action == null) {
                ctx.getLogger().notifyError(Message.localized("fastback.notify.invalid-input", actionRaw));
                return FAILURE;
            }
            final StoredConfig config = git.getRepository().getConfig();
            WorldConfig.setShutdownAction(config, action);
            config.save();
            ctx.getLogger().info("Set shutdown action to "+action);
            return SUCCESS;
        });
    }
}
