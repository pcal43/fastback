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
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.StoredConfig;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.logging.Message.localized;

/**
 * Command to set the snapshot retention policy.
 *
 * @author pcal
 * @since 0.2.0
 */
enum SetRetentionCommand implements Command {

    INSTANCE;

    private static final String COMMAND_NAME = "set-retention";

    @Override
    public void register(LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        registerSetRetentionCommand(argb, ctx, COMMAND_NAME, (cc, rt) -> setLocalPolicy(ctx, cc, rt));
    }

    private static int setLocalPolicy(ModContext ctx, CommandContext<ServerCommandSource> cc, RetentionPolicyType rpt) {
        return setRetentionPolicy(ctx, cc, rpt, WorldConfig::setLocalRetentionPolicy);
    }


    /**
     * Register a 'set retention' command that tab completes with all the policies and the policy arguments.
     * Broken out as a helper methods so this logic can be shared by set-retention and set-remote-retention.
     * <p>
     * FIXME? The command parsing here could be more user-friendly.  Not really clear how to implement
     * argument defaults.  Also a lot of noise from bugs like this: https://bugs.mojang.com/browse/MC-165562
     * Just generally not sure how to beat brigadier into submission here.
     */
    static void registerSetRetentionCommand(final LiteralArgumentBuilder<ServerCommandSource> argb,
                                            final ModContext ctx,
                                            final String commandName,
                                            final BiFunction<CommandContext<ServerCommandSource>, RetentionPolicyType, Integer> setPolicyFn) {
        final LiteralArgumentBuilder<ServerCommandSource> retainCommand =
                literal(commandName).requires(subcommandPermission(ctx, commandName));
        for (final RetentionPolicyType rpt : ctx.getRetentionPolicyTypes()) {
            final LiteralArgumentBuilder<ServerCommandSource> policyCommand = literal(rpt.getCommandName());
            policyCommand.executes(cc -> setPolicyFn.apply(cc, rpt));
            if (rpt.getParameters() != null) {
                for (RetentionPolicyType.Parameter param : rpt.getParameters()) {
                    policyCommand.then(argument(param.name(), param.type()).
                            executes(cc -> setPolicyFn.apply(cc, rpt)));
                }
            }
            retainCommand.then(policyCommand);
        }
        argb.then(retainCommand);
    }

    /**
     * Does the work to encode a policy configuration and set it in git configuration.
     * Broken out as a helper methods so this logic can be shared by set-retention and set-remote-retention.
     */
    public static int setRetentionPolicy(final ModContext ctx,
                                         final CommandContext<ServerCommandSource> cc,
                                         final RetentionPolicyType rpt,
                                         final BiConsumer<Config, String> setEncodedPolicyFn) {
        final Logger logger = commandLogger(ctx, cc.getSource());
        try {
            final Path worldSaveDir = ctx.getWorldDirectory();
            final Map<String, String> config = new HashMap<>();
            for (final RetentionPolicyType.Parameter p : rpt.getParameters()) {
                final Object val = cc.getArgument(p.name(), Object.class);
                config.put(p.name(), String.valueOf(val));
            }
            final String encodedPolicy = RetentionPolicyCodec.INSTANCE.encodePolicy(ctx, rpt, config);
            final RetentionPolicy rp =
                    RetentionPolicyCodec.INSTANCE.decodePolicy(ctx, RetentionPolicyType.getAvailable(), encodedPolicy);
            if (rp == null) {
                logger.internalError("Failed to decode policy " + encodedPolicy, new Exception());
                return FAILURE;
            }
            try (final Git git = Git.open(worldSaveDir.toFile())) {
                final StoredConfig gitConfig = git.getRepository().getConfig();
                setEncodedPolicyFn.accept(gitConfig, encodedPolicy);
                gitConfig.save();
                logger.chat(localized("fastback.chat.retention-policy-set"));
                logger.chat(rp.getDescription());
            } catch (Exception e) {
                logger.internalError("Command execution failed.", e);
                return FAILURE;
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.internalError("Failed to set retention policy", e);
            return FAILURE;
        }
    }
}
