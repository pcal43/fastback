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
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.WorldConfig;
import net.pcal.fastback.logging.Logger;
import net.pcal.fastback.utils.GitUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;

import java.nio.file.Path;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.ModContext.ExecutionLock.NONE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.gitOp;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.utils.FileUtils.mkdirs;

enum CreateFileRemoteCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "create-file-remote";
    private static final String ARGUMENT = "file-path";

    @Override
    public void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).then(
                                argument(ARGUMENT, StringArgumentType.greedyString()).
                                        executes(cc -> setFileRemote(ctx, cc))
                        )
        );
    }

    private static int setFileRemote(final ModContext ctx, final CommandContext<ServerCommandSource> cc) {
        final Logger log = commandLogger(ctx, cc.getSource());
        gitOp(ctx, NONE, log, git -> {
            final String targetPath = cc.getArgument(ARGUMENT, String.class);
            final Path fupHome = Path.of(targetPath);
            if (fupHome.toFile().exists()) {
                log.chatError(localized("fastback.chat.create-file-remote-dir-exists", fupHome.toString()));
                return;
            }
            mkdirs(fupHome);
            try (Git targetGit = Git.init().setBare(ctx.isFileRemoteBare()).setDirectory(fupHome.toFile()).call()) {
                final StoredConfig targetGitc = targetGit.getRepository().getConfig();
                targetGitc.setInt("pack", null, "window", 0);
                targetGitc.setInt("core", null, "bigFileThreshold", 1);
                targetGitc.save();
            }
            final String targetUrl = GitUtils.getFileUri(fupHome);
            final StoredConfig gitc = git.getRepository().getConfig();
            WorldConfig.setRemoteUrl(gitc, targetUrl);
            gitc.save();
            log.chat(localized("fastback.chat.create-file-remote-created", targetPath, targetUrl));
        });
        return SUCCESS;
    }
}
