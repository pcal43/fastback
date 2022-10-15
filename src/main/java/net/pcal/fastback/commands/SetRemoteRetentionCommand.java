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
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.retention.RetentionPolicy;
import net.pcal.fastback.retention.RetentionPolicyCodec;
import net.pcal.fastback.retention.RetentionPolicyType;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.commands.SetRetentionCommand.registerSetRetentionCommand;
import static net.pcal.fastback.commands.SetRetentionCommand.setRetentionPolicy;
import static net.pcal.fastback.logging.Message.localized;

/**
 * Command to set the snapshot retention policy for the remote.
 *
 * @author pcal
 * @since 0.2.0
 */
enum SetRemoteRetentionCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "set-remote-retention";

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        registerSetRetentionCommand(argb, ctx, COMMAND_NAME, (cc, rt) -> setRemotePolicy(ctx, cc, rt));
    }

    private static int setRemotePolicy(ModContext ctx, CommandContext<ServerCommandSource> cc, RetentionPolicyType rpt) {
        return setRetentionPolicy(ctx, cc, rpt, WorldConfig::setRemoteRetentionPolicy);
    }

}
