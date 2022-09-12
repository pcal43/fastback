package net.pcal.fastback.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
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
import static net.minecraft.text.Text.translatable;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;

public class HelpCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final HelpCommand c = new HelpCommand(ctx);
        argb.then(literal("help").executes(c::help).then(
                        argument("subcommand", StringArgumentType.word()).
                                suggests(new HelpTopicSuggestions(ctx)).
                                executes(c::helpSubcommand))
        );
    }

    static class HelpTopicSuggestions implements SuggestionProvider<ServerCommandSource> {

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

    private final ModContext ctx;

    private HelpCommand(final ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int help(CommandContext<ServerCommandSource> cc) {
        final Logger log = commandLogger(ctx, cc);
        StringWriter subcommands = null;
        for(final String available : getSubcommandNames(cc)) {
            if (subcommands == null) {
                subcommands = new StringWriter();
            } else {
                subcommands.append(", ");
            }
            subcommands.append(available);
        }
        log.notify(translatable("commands.fastback.help.subcommands", String.valueOf(subcommands)));
        return SUCCESS;
    }

    private int helpSubcommand(CommandContext<ServerCommandSource> cc) {
        final Logger log = commandLogger(ctx, cc);
        final Collection<CommandNode<ServerCommandSource>> subcommands = cc.getNodes().get(0).getNode().getChildren();
        final String subcommand = cc.getLastChild().getArgument("subcommand", String.class);
        for(String available : getSubcommandNames(cc)) {
            if (subcommand.equals(available)) {
                final String prefix = "/backup "+subcommand+": ";
                log.notify(translatable("commands.fastback."+subcommand+".help", prefix));
                return SUCCESS;
            }
        }
        log.notifyError(Text.literal("Invalid subcommand '"+subcommand+"'"));
        return FAILURE;
    }

    private static List<String> getSubcommandNames(CommandContext<ServerCommandSource> cc) {
        final List<String> out = new ArrayList<>();
        cc.getNodes().get(0).getNode().getChildren().forEach(node->out.add(node.getName()));
        return out;
    }



}
