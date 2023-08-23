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
import net.pcal.fastback.config.GitConfig;
import net.pcal.fastback.config.GitConfigKey;
import net.pcal.fastback.logging.UserLogger;
import net.pcal.fastback.logging.UserMessage;
import net.pcal.fastback.mod.Mod;
import net.pcal.fastback.retention.RetentionPolicy;
import net.pcal.fastback.retention.RetentionPolicyCodec;
import net.pcal.fastback.retention.RetentionPolicyType;
import org.eclipse.jgit.api.Git;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.fastback.commands.Commands.FAILURE;
import static net.pcal.fastback.commands.Commands.SUCCESS;
import static net.pcal.fastback.commands.Commands.commandLogger;
import static net.pcal.fastback.commands.Commands.getArgumentNicely;
import static net.pcal.fastback.commands.Commands.subcommandPermission;
import static net.pcal.fastback.config.GitConfigKey.LOCAL_RETENTION_POLICY;
import static net.pcal.fastback.logging.SystemLogger.syslog;

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
    public void register(LiteralArgumentBuilder<ServerCommandSource> argb, final Mod mod) {
        registerSetRetentionCommand(argb, mod, COMMAND_NAME, (cc, rt) -> setLocalPolicy(mod, cc, rt));
    }

    private static int setLocalPolicy(Mod mod, CommandContext<ServerCommandSource> cc, RetentionPolicyType rpt) {
        return setRetentionPolicy(mod, cc, rpt, LOCAL_RETENTION_POLICY);
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
                                            final Mod mod,
                                            final String commandName,
                                            final BiFunction<CommandContext<ServerCommandSource>, RetentionPolicyType, Integer> setPolicyFn) {
        final LiteralArgumentBuilder<ServerCommandSource> retainCommand =
                literal(commandName).requires(subcommandPermission(mod, commandName));
        for (final RetentionPolicyType rpt : RetentionPolicyType.getAvailable()) {
            final LiteralArgumentBuilder<ServerCommandSource> policyCommand = literal(rpt.getCommandName());
            policyCommand.executes(cc -> setPolicyFn.apply(cc, rpt));
            if (rpt.getParameters() != null) {
                for (RetentionPolicyType.Parameter<?> param : rpt.getParameters()) {
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
     * <p>
     * TODO this should probably move to Repo.
     */
    public static int setRetentionPolicy(final Mod mod,
                                         final CommandContext<ServerCommandSource> cc,
                                         final RetentionPolicyType rpt,
                                         final GitConfigKey confKey) {
        final UserLogger ulog = commandLogger(mod, cc.getSource());
        try {
            final Path worldSaveDir = mod.getWorldDirectory();
            final Map<String, String> config = new HashMap<>();
            for (final RetentionPolicyType.Parameter<?> p : rpt.getParameters()) {
                final Object val = getArgumentNicely(p.name(), p.clazz(), cc, ulog);
                if (val == null) return FAILURE;
                config.put(p.name(), String.valueOf(val));
            }
            final String encodedPolicy = RetentionPolicyCodec.INSTANCE.encodePolicy(rpt, config);
            final RetentionPolicy rp =
                    RetentionPolicyCodec.INSTANCE.decodePolicy(RetentionPolicyType.getAvailable(), encodedPolicy);
            if (rp == null) {
                syslog().error("Failed to decode policy " + encodedPolicy, new Exception());
                return FAILURE;
            }
            try (final Git jgit = Git.open(worldSaveDir.toFile())) {
                final GitConfig conf = GitConfig.load(jgit);
                conf.updater().set(confKey, encodedPolicy).save();
                ulog.message(UserMessage.localized("fastback.chat.retention-policy-set"));
                ulog.message(rp.getDescription());
            } catch (Exception e) {
                syslog().error("Command execution failed.", e);
                return FAILURE;
            }
            return SUCCESS;
        } catch (Exception e) {
            syslog().error("Failed to set retention policy", e);
            return FAILURE;
        }
    }
}
