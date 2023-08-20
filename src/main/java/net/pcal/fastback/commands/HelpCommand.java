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
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.fastback.ModContext;
import net.pcal.fastback.logging.Logger;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.Message.localized;
import static net.pcal.fastback.logging.Message.localizedError;

enum HelpCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "help";
    private static final String ARGUMENT = "subcommand";

    @Override
    public void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        argb.then(
                literal(COMMAND_NAME).
                        requires(subcommandPermission(ctx, COMMAND_NAME)).
                        executes(cc->help(ctx, cc)).
                        then(
                                argument(ARGUMENT, StringArgumentType.word()).
                                        suggests(new HelpTopicSuggestions(ctx)).
                                        executes(cc->helpSubcommand(ctx, cc))
                        )
        );
    }

    private static class HelpTopicSuggestions implements SuggestionProvider<ServerCommandSource> {

        private final ModContext ctx;

        HelpTopicSuggestions(ModContext ctx) {
            this.ctx = requireNonNull(ctx);
        }

        @Override
        public CompletableFuture<Suggestions> getSuggestions(final CommandContext<ServerCommandSource> cc,
                                                             final SuggestionsBuilder builder) {
            CompletableFuture<Suggestions> completableFuture = new CompletableFuture<>();
            getSubcommandNames(cc).forEach(builder::suggest);
            try {
                completableFuture.complete(builder.buildFuture().get());
            } catch (InterruptedException | ExecutionException e) {
                this.ctx.getLogger().internalError("looking up help topics", e);
                return null;
            }
            return completableFuture;
        }
    }

    static int help(final ModContext ctx, final CommandContext<ServerCommandSource> cc) {
        final Logger log = commandLogger(ctx, cc.getSource());
        StringWriter subcommands = null;
        for (final String available : getSubcommandNames(cc)) {
            if (subcommands == null) {
                subcommands = new StringWriter();
            } else {
                subcommands.append(", ");
            }
            subcommands.append(available);
        }
        log.chat(localized("fastback.help.subcommands", String.valueOf(subcommands)));
        return SUCCESS;
    }

    private int helpSubcommand(final ModContext ctx, final CommandContext<ServerCommandSource> cc) {
        final Logger log = commandLogger(ctx, cc.getSource());
        final Collection<CommandNode<ServerCommandSource>> subcommands = cc.getNodes().get(0).getNode().getChildren();
        final String subcommand = cc.getLastChild().getArgument(ARGUMENT, String.class);
        for (String available : getSubcommandNames(cc)) {
            if (subcommand.equals(available)) {
                final String prefix = "/backup " + subcommand + ": ";
                log.chat(localized("fastback.help.command." + subcommand, prefix));
                return SUCCESS;
            }
        }
        log.chat(localizedError("fastback.chat.invalid-input", subcommand));
        return FAILURE;
    }

    private static List<String> getSubcommandNames(CommandContext<ServerCommandSource> cc) {
        final List<String> out = new ArrayList<>();
        cc.getNodes().get(0).getNode().getChildren().forEach(node -> out.add(node.getName()));
        return out;
    }
}
