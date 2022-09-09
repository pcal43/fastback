package net.pcal.gitback.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.pcal.gitback.ModContext;
import net.pcal.gitback.WorldConfig;

import static java.util.Objects.requireNonNull;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.pcal.gitback.commands.Commands.FAILURE;
import static net.pcal.gitback.commands.Commands.SUCCESS;
import static net.pcal.gitback.commands.Commands.executeStandard;

public class RemoteCommand {

    public static void register(final LiteralArgumentBuilder<ServerCommandSource> argb, final ModContext ctx) {
        final RemoteCommand c = new RemoteCommand(ctx);
        argb.then(
                literal("remote").executes(c::showRemote).then(
                        literal("enable").executes(c::enable)).then(
                        literal("disable").executes(c::disable)).then(
                        argument("remote-url", StringArgumentType.greedyString()).
                                executes(c::setRemoteUrl))
        );
    }

    private final ModContext ctx;

    private RemoteCommand(ModContext context) {
        this.ctx = requireNonNull(context);
    }

    private int showRemote(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, tali) -> {
            final String remoteUrl = wc.getRemotePushUrl();
            final boolean enabled = wc.isRemoteBackupEnabled();
            if (enabled && remoteUrl != null) {
                tali.feedback("Remote backups are enabled to:");
                tali.feedback(remoteUrl);
                return SUCCESS;
            } else {
                tali.error("Remote backups are disabled.");
                if (remoteUrl != null) {
                    tali.feedback("Run '/backup remote enable' to enable remote backups to:");
                    tali.feedback(remoteUrl);
                } else {
                    tali.feedback("Run '/backup remote <remote-url>' to enable remote backups.");
                }
                return FAILURE;
            }
        });
    }

    private int enable(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, tali) -> {
            final String currentUrl = wc.getRemotePushUrl();
            final boolean currentEnabled = wc.isRemoteBackupEnabled();
            if (currentUrl == null) {
                tali.error("No remote URL is set.");
                tali.feedback("Run '/backup remote <remote-url>'");
                return FAILURE;
            } else if (currentEnabled) {
                tali.error("Remote backups are already enabled.");
                return FAILURE;
            } else {
                tali.feedback("Remote backups enabled to:");
                tali.feedback(currentUrl);
                WorldConfig.setRemoteBackupEnabled(gitc, true);
                gitc.save();
                return SUCCESS;
            }
        });
    }

    private int disable(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, tali) -> {
            final boolean currentEnabled = wc.isRemoteBackupEnabled();
            if (!currentEnabled) {
                tali.error("Remote backups are already disabled.");
                return FAILURE;
            } else {
                WorldConfig.setRemoteBackupEnabled(gitc, false);
                gitc.save();
                tali.feedback("Remote backups disabled.");
                return SUCCESS;
            }
        });
    }

    private int setRemoteUrl(final CommandContext<ServerCommandSource> cc) {
        return executeStandard(this.ctx, cc, (gitc, wc, tali) -> {
            final String newUrl = cc.getArgument("remote-url", String.class);
            final String currentUrl = wc.getRemotePushUrl();
            final boolean currentEnable = wc.isRemoteBackupEnabled();
            if (currentUrl != null && currentUrl.equals(newUrl)) {
                if (currentEnable) {
                    tali.feedback("Remote backups are already enabled to:");
                    tali.feedback(newUrl);
                    return SUCCESS;
                } else {
                    WorldConfig.setRemoteBackupEnabled(gitc, true);
                    gitc.save();
                    tali.feedback("Enabled remote backups to:");
                    tali.feedback(newUrl);
                }
            } else {
                WorldConfig.setRemoteUrl(gitc, newUrl);
                if (currentEnable) {
                    tali.feedback("Remote backup URL changed to " + newUrl);
                } else {
                    WorldConfig.setRemoteBackupEnabled(gitc, true);
                    tali.feedback("Enabled remote backups to:" + newUrl);
                    tali.feedback(newUrl);
                }
                gitc.save();
            }
            return SUCCESS;
        });
    }

}
