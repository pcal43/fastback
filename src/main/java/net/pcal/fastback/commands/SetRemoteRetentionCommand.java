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
import net.pcal.fastback.mod.Mod;
import net.pcal.fastback.retention.RetentionPolicyType;

import static net.pcal.fastback.commands.SetRetentionCommand.registerSetRetentionCommand;
import static net.pcal.fastback.commands.SetRetentionCommand.setRetentionPolicy;
import static net.pcal.fastback.config.FastbackConfigKey.REMOTE_RETENTION_POLICY;

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
    public void register(LiteralArgumentBuilder<ServerCommandSource> argb, final Mod mod) {
        registerSetRetentionCommand(argb, mod, COMMAND_NAME, (cc, rt) -> setRemotePolicy(mod, cc, rt));
    }

    private static int setRemotePolicy(Mod mod, CommandContext<ServerCommandSource> cc, RetentionPolicyType rpt) {
        return setRetentionPolicy(mod, cc, rpt, REMOTE_RETENTION_POLICY);
    }

}
